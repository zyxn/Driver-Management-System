package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"math/rand"
	"net/http"
	"net/url"
	"time"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/infrastructure/config"
	"driver-management-backend/internal/infrastructure/security"

	"github.com/joho/godotenv"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// ─────────────────────────────────────────────
// OSRM
// ─────────────────────────────────────────────

type OSRMResponse struct {
	Code   string `json:"code"`
	Routes []struct {
		Geometry struct {
			Coordinates [][]float64 `json:"coordinates"`
		} `json:"geometry"`
	} `json:"routes"`
}

// getOSRMRoute calls OSRM public API and returns []{lat, lng} polyline.
// On any error returns nil so caller can fallback.
func getOSRMRoute(fromLat, fromLng, toLat, toLng float64) [][2]float64 {
	endpoint := fmt.Sprintf(
		"https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
		fromLng, fromLat, toLng, toLat, // OSRM uses lng,lat order
	)

	client := &http.Client{Timeout: 8 * time.Second}
	resp, err := client.Get(endpoint)
	if err != nil {
		return nil
	}
	defer resp.Body.Close()

	var osrm OSRMResponse
	if err := json.NewDecoder(resp.Body).Decode(&osrm); err != nil {
		return nil
	}
	if osrm.Code != "Ok" || len(osrm.Routes) == 0 {
		return nil
	}

	coords := osrm.Routes[0].Geometry.Coordinates
	result := make([][2]float64, 0, len(coords))
	for _, c := range coords {
		if len(c) >= 2 {
			result = append(result, [2]float64{c[1], c[0]}) // convert to lat,lng
		}
	}
	return result
}

// ─────────────────────────────────────────────
// Pre-baked Jakarta road waypoints (fallback)
// ─────────────────────────────────────────────

// straightLineFallback returns a simple list of interpolated lat/lng points
// following a few intermediate road-ish nodes between two Jakarta points.
func straightLineFallback(fromLat, fromLng, toLat, toLng float64) [][2]float64 {
	const steps = 20
	points := make([][2]float64, 0, steps+1)
	for i := 0; i <= steps; i++ {
		t := float64(i) / float64(steps)
		lat := fromLat + (toLat-fromLat)*t
		lng := fromLng + (toLng-fromLng)*t
		// add small road-grid-style jitter so it doesn't look perfectly straight
		if i > 0 && i < steps {
			lat += (rand.Float64() - 0.5) * 0.002
			lng += (rand.Float64() - 0.5) * 0.002
		}
		points = append(points, [2]float64{lat, lng})
	}
	return points
}

// ─────────────────────────────────────────────
// Route cache (avoid duplicate OSRM calls)
// ─────────────────────────────────────────────

var routeCache = map[string][][2]float64{}

func routeCacheKey(fromLat, fromLng, toLat, toLng float64) string {
	return url.QueryEscape(fmt.Sprintf("%.4f,%.4f,%.4f,%.4f", fromLat, fromLng, toLat, toLng))
}

func getRoute(fromLat, fromLng, toLat, toLng float64) [][2]float64 {
	key := routeCacheKey(fromLat, fromLng, toLat, toLng)
	if cached, ok := routeCache[key]; ok {
		return cached
	}

	route := getOSRMRoute(fromLat, fromLng, toLat, toLng)
	if route == nil {
		log.Printf("  [OSRM fallback] %.4f,%.4f -> %.4f,%.4f", fromLat, fromLng, toLat, toLng)
		route = straightLineFallback(fromLat, fromLng, toLat, toLng)
	}

	routeCache[key] = route
	return route
}

// ─────────────────────────────────────────────
// Geometry helpers
// ─────────────────────────────────────────────

func haversineMeters(lat1, lng1, lat2, lng2 float64) float64 {
	const R = 6371000
	phi1 := lat1 * math.Pi / 180
	phi2 := lat2 * math.Pi / 180
	dphi := (lat2 - lat1) * math.Pi / 180
	dlam := (lng2 - lng1) * math.Pi / 180
	a := math.Sin(dphi/2)*math.Sin(dphi/2) +
		math.Cos(phi1)*math.Cos(phi2)*math.Sin(dlam/2)*math.Sin(dlam/2)
	return R * 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))
}

// polylineLength returns cumulative distances in metres for each point.
func polylineLength(pts [][2]float64) []float64 {
	dist := make([]float64, len(pts))
	for i := 1; i < len(pts); i++ {
		dist[i] = dist[i-1] + haversineMeters(pts[i-1][0], pts[i-1][1], pts[i][0], pts[i][1])
	}
	return dist
}

// interpolatePolyline returns the lat/lng at distance d metres along the polyline.
func interpolatePolyline(pts [][2]float64, cumDist []float64, d float64) (float64, float64) {
	total := cumDist[len(cumDist)-1]
	if d <= 0 {
		return pts[0][0], pts[0][1]
	}
	if d >= total {
		return pts[len(pts)-1][0], pts[len(pts)-1][1]
	}
	// binary search
	lo, hi := 0, len(cumDist)-1
	for lo+1 < hi {
		mid := (lo + hi) / 2
		if cumDist[mid] <= d {
			lo = mid
		} else {
			hi = mid
		}
	}
	seg := cumDist[hi] - cumDist[lo]
	if seg == 0 {
		return pts[lo][0], pts[lo][1]
	}
	t := (d - cumDist[lo]) / seg
	lat := pts[lo][0] + (pts[hi][0]-pts[lo][0])*t
	lng := pts[lo][1] + (pts[hi][1]-pts[lo][1])*t
	return lat, lng
}

// ─────────────────────────────────────────────
// Jakarta places
// ─────────────────────────────────────────────

type Place struct {
	Name     string
	Lat, Lng float64
}

var places = []Place{
	{"Monas, Jakarta Pusat", -6.175392, 106.827153},
	{"Grand Indonesia, Jakarta", -6.195396, 106.823067},
	{"Taman Mini Indonesia Indah", -6.302500, 106.895370},
	{"Ancol, Jakarta Utara", -6.122435, 106.842674},
	{"Blok M, Jakarta Selatan", -6.244750, 106.798389},
	{"Kota Tua, Jakarta Barat", -6.135200, 106.813301},
	{"Senayan City, Jakarta", -6.225556, 106.799444},
	{"Plaza Indonesia, Jakarta", -6.192778, 106.822778},
	{"Kelapa Gading, Jakarta Utara", -6.158056, 106.909722},
	{"Pondok Indah Mall, Jakarta Selatan", -6.265556, 106.783889},
	{"Mangga Dua, Jakarta Utara", -6.138889, 106.823611},
	{"Tanah Abang, Jakarta Pusat", -6.185278, 106.812500},
	{"Cikini, Jakarta Pusat", -6.192500, 106.841944},
	{"Menteng, Jakarta Pusat", -6.195833, 106.833056},
	{"Kemang, Jakarta Selatan", -6.265000, 106.816667},
	{"Harmoni, Jakarta Pusat", -6.166667, 106.816667},
	{"Grogol, Jakarta Barat", -6.168889, 106.790000},
	{"Sunter, Jakarta Utara", -6.143889, 106.870556},
	{"Cilandak, Jakarta Selatan", -6.286944, 106.796944},
	{"Pulo Gadung, Jakarta Timur", -6.183056, 106.893056},
}

var reportDescriptions = []string{
	"Pengiriman barang berhasil diselesaikan",
	"Pengantaran paket selesai tepat waktu",
	"Pengambilan barang dari gudang",
	"Pengiriman dokumen penting",
	"Kunjungan ke klien",
	"Pengantaran paket express",
	"Pengiriman barang elektronik",
	"Pengantaran makanan",
	"Pengiriman paket COD",
	"Verifikasi alamat pengiriman",
	"Pengambilan return barang",
	"Pengiriman ke kantor cabang",
}

// ─────────────────────────────────────────────
// Speed model (km/h → m/s)
// ─────────────────────────────────────────────

// realisticSpeedMS returns speed in m/s based on time of day (Jakarta traffic).
func realisticSpeedMS(t time.Time) float64 {
	h := t.Hour()
	var kmh float64
	switch {
	case h >= 7 && h <= 9: // morning rush
		kmh = 10 + rand.Float64()*10
	case h >= 17 && h <= 19: // evening rush
		kmh = 8 + rand.Float64()*10
	case h >= 22 || h <= 5: // night, fast
		kmh = 40 + rand.Float64()*20
	default: // normal
		kmh = 25 + rand.Float64()*15
	}
	return kmh * 1000 / 3600
}

// ─────────────────────────────────────────────
// Main
// ─────────────────────────────────────────────

func main() {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using environment variables")
	}

	cfg := config.NewConfig()

	db, err := gorm.Open(postgres.Open(cfg.Database.DSN()), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	log.Println("Starting seeding process...")

	if err := clearData(db); err != nil {
		log.Fatal("Failed to clear data:", err)
	}
	if err := seedUsers(db); err != nil {
		log.Fatal("Failed to seed users:", err)
	}
	if err := seedTrackingData(db); err != nil {
		log.Fatal("Failed to seed tracking data:", err)
	}

	log.Println("Seeding completed successfully!")
}

// ─────────────────────────────────────────────
// clearData / seedUsers  (same as before)
// ─────────────────────────────────────────────

func clearData(db *gorm.DB) error {
	for _, tbl := range []string{"alerts", "reports", "locations", "users"} {
		if err := db.Exec("DELETE FROM " + tbl).Error; err != nil {
			return err
		}
		log.Println("Cleared", tbl)
	}
	return nil
}

func seedUsers(db *gorm.DB) error {
	log.Println("Seeding users...")

	hashedPassword, err := security.HashPassword("password123")
	if err != nil {
		return err
	}

	var users []entity.User

	// 1 Operator
	users = append(users, entity.User{
		Username:  "operator1",
		Email:     "operator1@example.com",
		Password:  hashedPassword,
		FullName:  "Operator Utama",
		Phone:     "081234567891",
		Role:      "operator",
		Status:    "active",
		FCMToken:  nil,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	})

	// 2 Drivers
	driverNames := []string{"Budi Santoso", "Ahmad Wijaya"}
	
	for i := 1; i <= 2; i++ {
		licenseNo := fmt.Sprintf("B%010d", 1000000000+i*12345)
		
		users = append(users, entity.User{
			Username:  fmt.Sprintf("driver%d", i),
			Email:     fmt.Sprintf("driver%d@example.com", i),
			Password:  hashedPassword,
			FullName:  driverNames[i-1],
			Phone:     fmt.Sprintf("08234567%04d", 8900+i),
			LicenseNo: &licenseNo,
			Role:      "driver",
			Status:    "active",
			FCMToken:  nil,
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		})
	}

	for _, u := range users {
		if err := db.Create(&u).Error; err != nil {
			return err
		}
	}
	log.Printf("Created %d users", len(users))
	return nil
}

// ─────────────────────────────────────────────
// seedTrackingData — realistic GPS simulation
// ─────────────────────────────────────────────

func seedTrackingData(db *gorm.DB) error {
	log.Println("Seeding realistic tracking data (H-3 to now)...")

	var drivers []entity.User
	if err := db.Where("role = ?", "driver").Find(&drivers).Error; err != nil {
		return err
	}
	if len(drivers) == 0 {
		log.Println("No drivers found")
		return nil
	}

	rand.Seed(time.Now().UnixNano())

	now := time.Now().UTC()
	rangeStart := now.AddDate(0, 0, -3).Truncate(24 * time.Hour) // H-3 00:00 UTC
	jakartaTZ := time.FixedZone("Asia/Jakarta", 7*3600)

	var (
		locationBuf []entity.Location
		reportBuf   []entity.Report
		totalLoc    int
		totalRep    int
	)

	flushLocations := func(force bool) error {
		if len(locationBuf) >= 5000 || (force && len(locationBuf) > 0) {
			if err := db.CreateInBatches(locationBuf, 5000).Error; err != nil {
				return err
			}
			totalLoc += len(locationBuf)
			log.Printf("  flushed locations, total=%d", totalLoc)
			locationBuf = locationBuf[:0]
		}
		return nil
	}
	flushReports := func(force bool) error {
		if len(reportBuf) >= 500 || (force && len(reportBuf) > 0) {
			if err := db.CreateInBatches(reportBuf, 500).Error; err != nil {
				return err
			}
			totalRep += len(reportBuf)
			log.Printf("  flushed reports, total=%d", totalRep)
			reportBuf = reportBuf[:0]
		}
		return nil
	}

	totalDays := 4 // H-3, H-2, H-1, H

	for _, driver := range drivers {
		if driver.Status == "inactive" && rand.Float32() < 0.7 {
			continue
		}

		workDays := 3 + rand.Intn(2) // 3–4 hari

		log.Printf("Driver %s: %d workdays", driver.FullName, workDays)

		usedDays := map[int]bool{}

		for day := 0; day < workDays; day++ {
			var randomDay int
			for {
				randomDay = rand.Intn(totalDays)
				if !usedDays[randomDay] {
					usedDays[randomDay] = true
					break
				}
			}

			workDate := rangeStart.AddDate(0, 0, randomDay)

			// cek apakah hari ini
			isToday := workDate.Year() == now.Year() &&
				workDate.Month() == now.Month() &&
				workDate.Day() == now.Day()

			// shift start
			startHour := 8 + rand.Intn(2)
			startMin := rand.Intn(60)

			shiftStart := time.Date(workDate.Year(), workDate.Month(), workDate.Day(),
				startHour, startMin, 0, 0, time.UTC)

			// shift end
			endHour := 16 + rand.Intn(3)
			endMin := rand.Intn(60)

			var shiftEnd time.Time
			if isToday {
				shiftEnd = now
			} else {
				shiftEnd = time.Date(workDate.Year(), workDate.Month(), workDate.Day(),
					endHour, endMin, 0, 0, time.UTC)
			}

			// clamp
			if shiftEnd.After(now) {
				shiftEnd = now
			}

			if shiftEnd.Before(shiftStart) {
				continue
			}

			// stops
			numStops := 3 + rand.Intn(4)

			shuffled := make([]Place, len(places))
			copy(shuffled, places)
			rand.Shuffle(len(shuffled), func(i, j int) {
				shuffled[i], shuffled[j] = shuffled[j], shuffled[i]
			})

			depot := shuffled[0]
			stops := shuffled[1 : 1+numStops]

			type Waypoint struct {
				Place
				ArriveAt time.Time
			}

			totalShiftSecs := shiftEnd.Sub(shiftStart).Seconds()
			gap := totalShiftSecs / float64(numStops+1)

			waypoints := []Waypoint{{Place: depot, ArriveAt: shiftStart}}

			for i, stop := range stops {
				offset := gap*float64(i+1) + (rand.Float64()-0.5)*gap*0.3
				arriveAt := shiftStart.Add(time.Duration(offset) * time.Second)
				if arriveAt.After(shiftEnd) {
					arriveAt = shiftEnd
				}
				waypoints = append(waypoints, Waypoint{Place: stop, ArriveAt: arriveAt})
			}

			waypoints = append(waypoints, Waypoint{Place: depot, ArriveAt: shiftEnd})

			currentTime := shiftStart

			for segIdx := 0; segIdx < len(waypoints)-1; segIdx++ {
				from := waypoints[segIdx]
				to := waypoints[segIdx+1]

				polyline := getRoute(from.Lat, from.Lng, to.Lat, to.Lng)
				cumDist := polylineLength(polyline)
				totalDist := cumDist[len(cumDist)-1]

				segDuration := to.ArriveAt.Sub(from.ArriveAt)
				if segDuration <= 0 {
					continue
				}

				const tickSec = 30
				ticksInSeg := int(segDuration.Seconds() / tickSec)
				if ticksInSeg < 1 {
					ticksInSeg = 1
				}

				for tick := 0; tick <= ticksInSeg; tick++ {
					progress := float64(tick) / float64(ticksInSeg)

					targetDist := totalDist * progress
					lat, lng := interpolatePolyline(polyline, cumDist, targetDist)

					lat += (rand.Float64() - 0.5) * 0.00003
					lng += (rand.Float64() - 0.5) * 0.00003

					speedMS := realisticSpeedMS(currentTime)
					speedKmh := speedMS * 3.6

					localTime := currentTime.In(jakartaTZ)

					locationBuf = append(locationBuf, entity.Location{
						UserID:          driver.ID,
						Latitude:        lat,
						Longitude:       lng,
						Speed:           speedKmh,
						Accuracy:        4 + rand.Float64()*8,
						RecordedAtUTC:   currentTime,
						Timezone:        "Asia/Jakarta",
						RecordedAtLocal: localTime,
						CreatedAt:       time.Now(),
					})

					if err := flushLocations(false); err != nil {
						return err
					}

					currentTime = currentTime.Add(tickSec * time.Second)
					if currentTime.After(now) {
						break
					}
				}

				// report
				if segIdx > 0 && segIdx < len(waypoints)-2 {
					localArrival := to.ArriveAt.In(jakartaTZ)

					reportBuf = append(reportBuf, entity.Report{
						UserID:          driver.ID,
						PlaceName:       to.Name,
						Latitude:        to.Lat,
						Longitude:       to.Lng,
						Description:     reportDescriptions[rand.Intn(len(reportDescriptions))],
						ReportedAtUTC:   to.ArriveAt,
						Timezone:        "Asia/Jakarta",
						ReportedAtLocal: localArrival,
						CreatedAt:       time.Now(),
						UpdatedAt:       time.Now(),
					})

					if err := flushReports(false); err != nil {
						return err
					}
				}
			}
		}
	}

	if err := flushLocations(true); err != nil {
		return err
	}
	if err := flushReports(true); err != nil {
		return err
	}

	log.Printf("Done! Total locations=%d, reports=%d", totalLoc, totalRep)
	return nil
}
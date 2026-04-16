package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"time"
)

type LocationUpdate struct {
	UserID          uint    `json:"user_id"`
	Latitude        float64 `json:"latitude"`
	Longitude       float64 `json:"longitude"`
	Speed           float64 `json:"speed"`
	Accuracy        float64 `json:"accuracy"`
	Heading         float64 `json:"heading"`
	RecordedAtUTC   string  `json:"recorded_at_utc"`
	Timezone        string  `json:"timezone"`
	RecordedAtLocal string  `json:"recorded_at_local"`
}

// Jakarta area bounds
const (
	jakartaLatMin  = -6.35
	jakartaLatMax  = -6.10
	jakartaLngMin  = 106.70
	jakartaLngMax  = 106.95
	maxSpeed       = 80.0
	minSpeed       = 0.0
	updateInterval = 30 * time.Second
)

func main() {
	apiURL := "http://localhost:3000/api/v1"

	// Use driver IDs 50-55 (adjust based on your seeded data)
	driverIDs := []uint{50, 51, 52, 53, 54, 55}

	log.Printf("========================================")
	log.Printf("Mock Location Generator (%d Drivers)", len(driverIDs))
	log.Printf("========================================")
	log.Printf("API URL: %s", apiURL)
	log.Printf("Update interval: %v", updateInterval)
	log.Printf("Drivers: %v", driverIDs)
	log.Printf("No authentication - for testing only")
	log.Println()

	// Initialize random positions for each driver
	driverPositions := make(map[uint]*DriverPosition)
	for _, driverID := range driverIDs {
		driverPositions[driverID] = &DriverPosition{
			Lat:     randomFloat(jakartaLatMin, jakartaLatMax),
			Lng:     randomFloat(jakartaLngMin, jakartaLngMax),
			Speed:   randomFloat(20, 60),
			Heading: randomFloat(0, 360),
		}
		log.Printf("Driver %d initialized at: Lat=%.6f, Lng=%.6f", 
			driverID, driverPositions[driverID].Lat, driverPositions[driverID].Lng)
	}

	log.Println()
	log.Println("Starting location updates... (Press Ctrl+C to stop)")
	log.Println()

	// Continuously send location updates
	ticker := time.NewTicker(updateInterval)
	defer ticker.Stop()

	for {
		for _, driverID := range driverIDs {
			pos := driverPositions[driverID]
			
			// Update position (simulate movement)
			pos.Move()

			// Send location update via HTTP POST
			if err := sendLocationUpdate(apiURL, driverID, pos); err != nil {
				log.Printf("❌ Driver %d: Error - %v", driverID, err)
			} else {
				log.Printf("✓ Driver %d: Lat=%.6f, Lng=%.6f, Speed=%.1f km/h, Heading=%.1f°",
					driverID, pos.Lat, pos.Lng, pos.Speed, pos.Heading)
			}
		}

		log.Println()
		<-ticker.C
	}
}

type DriverPosition struct {
	Lat     float64
	Lng     float64
	Speed   float64
	Heading float64
}

func (p *DriverPosition) Move() {
	// Simulasi perubahan kecepatan yang lebih realistis
	// Kadang nambah, kadang berkurang, kadang berhenti
	rand := rand.Float64()
	
	if rand < 0.1 {
		// 10% kemungkinan berhenti (lampu merah, macet)
		p.Speed = randomFloat(0, 5)
	} else if rand < 0.3 {
		// 20% kemungkinan melambat
		p.Speed -= randomFloat(5, 15)
	} else if rand < 0.6 {
		// 30% kemungkinan nambah kecepatan
		p.Speed += randomFloat(5, 20)
	} else {
		// 40% kemungkinan kecepatan stabil dengan variasi kecil
		p.Speed += randomFloat(-3, 3)
	}

	// Batasi kecepatan
	if p.Speed < minSpeed {
		p.Speed = minSpeed
	}
	if p.Speed > maxSpeed {
		p.Speed = maxSpeed
	}

	// Ubah heading secara smooth (belok perlahan)
	headingChange := randomFloat(-20, 20)
	p.Heading += headingChange
	if p.Heading < 0 {
		p.Heading += 360
	}
	if p.Heading >= 360 {
		p.Heading -= 360
	}

	// Hitung posisi baru berdasarkan kecepatan dan heading
	// Speed dalam km/h, konversi ke derajat per update interval
	distanceKm := p.Speed * (updateInterval.Seconds() / 3600.0)
	distanceDeg := distanceKm / 111.0 // 1 derajat ≈ 111 km

	// Hitung pergerakan berdasarkan heading
	headingRad := p.Heading * (3.14159 / 180.0)
	p.Lat += distanceDeg * -1 * (headingRad / 3.14159) // Simplified movement
	p.Lng += distanceDeg * (headingRad / 3.14159)

	// Tetap dalam batas Jakarta
	if p.Lat < jakartaLatMin {
		p.Lat = jakartaLatMin
		p.Heading = randomFloat(0, 180)
	}
	if p.Lat > jakartaLatMax {
		p.Lat = jakartaLatMax
		p.Heading = randomFloat(180, 360)
	}
	if p.Lng < jakartaLngMin {
		p.Lng = jakartaLngMin
		p.Heading = randomFloat(270, 450)
		if p.Heading >= 360 {
			p.Heading -= 360
		}
	}
	if p.Lng > jakartaLngMax {
		p.Lng = jakartaLngMax
		p.Heading = randomFloat(90, 270)
	}
}

func sendLocationUpdate(apiURL string, driverID uint, pos *DriverPosition) error {
	now := time.Now()
	
	location := LocationUpdate{
		UserID:          driverID,
		Latitude:        pos.Lat,
		Longitude:       pos.Lng,
		Speed:           pos.Speed,
		Accuracy:        randomFloat(5, 20),
		Heading:         pos.Heading,
		RecordedAtUTC:   now.UTC().Format(time.RFC3339),
		Timezone:        "Asia/Jakarta",
		RecordedAtLocal: now.Format(time.RFC3339),
	}

	jsonData, err := json.Marshal(location)
	if err != nil {
		return err
	}

	req, err := http.NewRequest("POST", apiURL+"/locations/track", bytes.NewBuffer(jsonData))
	if err != nil {
		return err
	}

	req.Header.Set("Content-Type", "application/json")
	// No Authorization header - bypass auth for testing

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusOK {
		var errResp map[string]interface{}
		json.NewDecoder(resp.Body).Decode(&errResp)
		return fmt.Errorf("status %d: %v", resp.StatusCode, errResp)
	}

	return nil
}

func randomFloat(min, max float64) float64 {
	return min + rand.Float64()*(max-min)
}

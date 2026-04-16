# Mock Location Generator

Script untuk generate data lokasi driver secara real-time untuk testing.

## 🎯 Cara Kerja

1. **Mobile App/Mock Generator** → Kirim lokasi via **HTTP POST** ke `/api/v1/locations/track`
2. **Backend** → Simpan ke database & broadcast via **WebSocket** ke semua dashboard yang terhubung
3. **Dashboard** → Terima update real-time via **WebSocket** dan update map

## 🚀 Cara Menggunakan

### 1. Pastikan Backend Running
```bash
cd backend
go run main.go
# atau
./driver-management-backend.exe
```

### 2. Jalankan Mock Generator (No Auth Required!)

**Windows:**
```bash
cd backend
run-mock-location.bat
```

**Linux/Mac:**
```bash
cd backend
go run cmd/mock-location/main.go
```

### 3. Buka Dashboard
Buka browser ke `http://localhost:5173/dashboard` dan lihat marker driver bergerak secara real-time!

## ✨ Fitur

- ✅ **3 driver** bergerak simultan di area Jakarta
- ✅ **No authentication required** - langsung kirim data
- ✅ **HTTP POST** untuk kirim lokasi (seperti dari mobile app)
- ✅ **WebSocket broadcast** otomatis ke semua dashboard
- ✅ Update setiap **5 detik**
- ✅ Kecepatan & heading realistis

## 📡 API Endpoint

### POST /api/v1/locations/track
Kirim lokasi driver (no auth required untuk testing)

**Request Body:**
```json
{
  "user_id": 2,
  "latitude": -6.2088,
  "longitude": 106.8456,
  "speed": 45.5,
  "accuracy": 10.2,
  "heading": 180.5,
  "recorded_at_utc": "2024-01-01T10:00:00Z",
  "timezone": "Asia/Jakarta",
  "recorded_at_local": "2024-01-01T17:00:00+07:00"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "user_id": 2,
    "latitude": -6.2088,
    "longitude": 106.8456,
    "speed": 45.5,
    "accuracy": 10.2,
    "heading": 180.5,
    "recorded_at_utc": "2024-01-01T10:00:00Z",
    "timezone": "Asia/Jakarta",
    "created_at": "2024-01-01T10:00:00Z"
  }
}
```

## 🔧 Konfigurasi

Edit `cmd/mock-location/main.go`:

```go
// Ubah jumlah driver
driverIDs := []uint{2, 3, 4, 5, 6}

// Ubah interval update
updateInterval = 3 * time.Second

// Ubah area simulasi
const (
	jakartaLatMin  = -6.35
	jakartaLatMax  = -6.10
	jakartaLngMin  = 106.70
	jakartaLngMax  = 106.95
)
```

## 📱 Integrasi dengan Mobile App

Dari mobile app (Android/iOS), kirim lokasi dengan format yang sama:

**Kotlin (Android):**
```kotlin
data class LocationUpdate(
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Double,
    val heading: Double,
    val recorded_at_utc: String,
    val timezone: String,
    val recorded_at_local: String
)

// Kirim ke backend
val location = LocationUpdate(
    user_id = currentUserId,
    latitude = location.latitude,
    longitude = location.longitude,
    speed = location.speed,
    accuracy = location.accuracy,
    heading = location.bearing.toDouble(),
    recorded_at_utc = Instant.now().toString(),
    timezone = "Asia/Jakarta",
    recorded_at_local = LocalDateTime.now().toString()
)

// POST ke http://your-server:8080/api/v1/locations/track
```

## 🐛 Troubleshooting

**Mock generator error "connection refused"**
- Pastikan backend running di port 8080

**Marker tidak muncul di dashboard**
- Cek browser console untuk error
- Pastikan WebSocket connected (lihat status indicator hijau)
- Cek network tab untuk request `/locations/latest`

**Data tidak real-time**
- Pastikan WebSocket connection berhasil
- Cek backend logs untuk broadcast messages

## 📊 Flow Diagram

```
Mobile App / Mock Generator
         |
         | HTTP POST /api/v1/locations/track
         ↓
    Backend Server
         |
         ├─→ Save to Database
         |
         └─→ Broadcast via WebSocket
                    |
                    ↓
              Dashboard (Browser)
                    |
                    └─→ Update Map Markers
```

# Testing Real-time Location Tracking

Panduan lengkap untuk testing fitur real-time location tracking dengan WebSocket.

## 🚀 Quick Start

### 1. Start Backend Server
```bash
cd backend
go run main.go
```

Backend akan running di `http://localhost:8080`

### 2. Start Frontend
```bash
cd frontend
npm run dev
```

Frontend akan running di `http://localhost:5173`

### 3. Login ke Aplikasi
1. Buka browser ke `http://localhost:5173/login`
2. Login dengan credentials:
   - Username: `admin`
   - Password: `admin123`

### 4. Dapatkan JWT Token

**Cara 1: Dari Browser Console**
```javascript
// Buka Developer Tools (F12) > Console
localStorage.getItem('token')
```

**Cara 2: Dari API**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Copy JWT token dari response.

### 5. Jalankan Mock Location Generator

**Windows:**
```bash
cd backend
run-mock-location.bat
# Paste JWT token ketika diminta
```

**Linux/Mac:**
```bash
cd backend
export AUTH_TOKEN="your_jwt_token_here"
go run cmd/mock-location/main.go
```

### 6. Lihat Real-time Tracking!
1. Buka `http://localhost:5173/dashboard`
2. Lihat status indicator berubah menjadi "Live Tracking Active" (hijau)
3. Marker driver akan muncul dan bergerak di map setiap 5 detik!

## 📊 Apa yang Terjadi?

### Backend
1. **WebSocket Server** (`/ws/locations`) menerima koneksi dari frontend
2. **Mock Generator** mengirim location updates via HTTP POST ke `/api/v1/locations/track`
3. **Location Handler** menyimpan data ke database
4. **WebSocket Handler** broadcast update ke semua connected clients

### Frontend
1. **Dashboard** load initial locations dari `/api/v1/locations/latest`
2. **WebSocket Client** connect ke `ws://localhost:8080/ws/locations`
3. Menerima real-time updates dan update markers di map
4. Status indicator menunjukkan connection status

## 🎯 Fitur yang Bisa Dilihat

- ✅ **Real-time marker updates** - Driver bergerak di map
- ✅ **Connection status indicator** - Hijau = connected, Kuning = connecting, Merah = disconnected
- ✅ **Driver info popup** - Klik marker untuk lihat detail
- ✅ **Speed & heading** - Kecepatan dan arah pergerakan
- ✅ **Auto-reconnect** - Jika koneksi terputus, otomatis reconnect setelah 5 detik
- ✅ **Multiple drivers** - Simulasi 5 driver bergerak bersamaan

## 🔧 Konfigurasi

### Ubah Jumlah Driver
Edit `backend/cmd/mock-location/main.go`:
```go
// Line ~40
driverIDs := []uint{2, 3, 4, 5, 6, 7, 8} // Tambah ID driver
```

### Ubah Update Interval
```go
// Line ~25
updateInterval = 3 * time.Second // Ubah dari 5 detik ke 3 detik
```

### Ubah Area Simulasi
```go
// Line ~18-21
const (
	jakartaLatMin  = -6.35  // Ubah bounds
	jakartaLatMax  = -6.10
	jakartaLngMin  = 106.70
	jakartaLngMax  = 106.95
)
```

## 🐛 Troubleshooting

### Marker Tidak Muncul

**1. Cek WebSocket Connection**
- Buka Developer Tools > Console
- Lihat apakah ada error "WebSocket connection failed"
- Pastikan backend running di port 8080

**2. Cek Initial Data Load**
```javascript
// Di browser console
fetch('http://localhost:8080/api/v1/locations/latest', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
}).then(r => r.json()).then(console.log)
```

**3. Cek Mock Generator**
- Pastikan mock generator running dan tidak ada error
- Lihat log: "✓ Driver X: Lat=..., Lng=..., Speed=..."

### WebSocket Disconnected

**Penyebab:**
- Backend restart
- Network issue
- CORS issue

**Solusi:**
- Frontend akan auto-reconnect setelah 5 detik
- Refresh page jika masih bermasalah
- Cek backend logs untuk error

### CORS Error

Pastikan backend CORS config di `backend/internal/app/app.go` sudah benar:
```go
app.Use(cors.New(cors.Config{
    AllowOrigins: "*",
    AllowHeaders: "Origin, Content-Type, Accept, Authorization",
    AllowMethods: "GET, POST, PUT, PATCH, DELETE, OPTIONS",
}))
```

## 📝 API Endpoints

### REST API
- `POST /api/v1/locations/track` - Track location
- `GET /api/v1/locations/latest` - Get all drivers' latest locations
- `GET /api/v1/locations/user/:userId` - Get location history
- `GET /api/v1/locations/user/:userId/latest` - Get user's latest location

### WebSocket
- `ws://localhost:8080/ws/locations` - Real-time location updates

### WebSocket Message Format

**Client → Server:**
```json
{
  "type": "ping"
}
```

**Server → Client:**
```json
{
  "type": "location_update",
  "user_id": 2,
  "data": {
    "id": 123,
    "user_id": 2,
    "user": {
      "id": 2,
      "username": "driver1",
      "full_name": "John Doe"
    },
    "latitude": -6.2088,
    "longitude": 106.8456,
    "speed": 45.5,
    "accuracy": 10.2,
    "heading": 180.5,
    "recorded_at_utc": "2024-01-01T10:00:00Z",
    "timezone": "Asia/Jakarta"
  },
  "timestamp": "2024-01-01T10:00:00Z"
}
```

## 🎨 Customization

### Ubah Marker Style
Edit `frontend/src/lib/components/map.svelte`:
```javascript
const createCustomIcon = (label: string = '') => {
	const color = '#3b82f6'; // Ubah warna marker
	// ...
}
```

### Ubah Update Frequency di Frontend
Edit `frontend/src/lib/api/locations.ts`:
```javascript
// Line ~120
pingInterval = setInterval(() => {
	// ...
}, 30000); // Ubah dari 30 detik
```

## 🚦 Next Steps

1. **Add Route History** - Tampilkan trail/path pergerakan driver
2. **Add Geofencing** - Alert ketika driver keluar dari area
3. **Add Speed Alerts** - Alert ketika driver over speed
4. **Add Driver Filtering** - Filter driver by status/name
5. **Add Heatmap** - Visualisasi area dengan traffic tinggi

## 📚 Resources

- [Fiber WebSocket Docs](https://docs.gofiber.io/api/middleware/websocket)
- [Leaflet.js Docs](https://leafletjs.com/)
- [SvelteKit Docs](https://kit.svelte.dev/)

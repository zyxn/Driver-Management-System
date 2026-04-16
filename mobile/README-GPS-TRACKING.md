# GPS Tracking System - Mobile to Backend

## Overview
Sistem GPS tracking yang mengirim data lokasi real-time dari mobile app ke backend Go setiap 10 detik.

## Arsitektur

### Mobile App (Android - Kotlin)
```
LocationTrackingService
    ↓ (setiap 5 detik dapat GPS update)
    ↓ (throttle: kirim ke backend setiap 10 detik)
SendLocationUseCase
    ↓
LocationRepository
    ↓
ApiService (Retrofit)
    ↓
Backend API
```

### Backend (Go)
```
POST /locations/track
    ↓
LocationHandler.TrackLocation
    ↓
LocationUseCase.TrackLocation
    ↓
LocationRepository.Create
    ↓
PostgreSQL (locations table)
```

## Data Flow

### 1. Mobile App Mengumpulkan GPS Data
`LocationTrackingService` menggunakan Google Play Services FusedLocationProvider untuk mendapatkan lokasi dengan:
- **Update Interval**: 5 detik (GPS update)
- **Send Interval**: 30 detik (kirim ke backend)
- **Accuracy**: HIGH_ACCURACY
- **Min Update Interval**: 2 detik

### 2. Data yang Dikumpulkan
Dari Android Location object:
- `latitude` - Koordinat lintang
- `longitude` - Koordinat bujur
- `speed` - Kecepatan (m/s → dikonversi ke km/h)
- `accuracy` - Akurasi GPS dalam meter
- `bearing` - Arah pergerakan (0-360 derajat) - nullable
- `altitude` - Ketinggian dari permukaan laut - nullable
- `time` - Timestamp dari GPS

### 3. Konversi Data untuk Backend

#### Timestamp Conversion
```kotlin
// GPS timestamp (milliseconds)
val timestamp = location.time

// Convert to UTC (RFC3339)
val instant = Instant.ofEpochMilli(timestamp)
val recordedAtUtc = DateTimeFormatter.ISO_INSTANT.format(instant)
// Output: "2024-01-15T10:30:45.123Z"

// Convert to Local Time with Timezone
val localZoneId = ZoneId.systemDefault()
val localDateTime = instant.atZone(localZoneId)
val recordedAtLocal = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(localDateTime)
// Output: "2024-01-15T17:30:45.123+07:00"

// Timezone name
val timezone = localZoneId.id
// Output: "Asia/Jakarta"
```

#### Speed Conversion
```kotlin
// Android gives speed in m/s, backend expects km/h
val speedKmh = location.speed * 3.6
```

### 4. Request Format ke Backend

```json
{
  "user_id": 1,
  "latitude": -6.200000,
  "longitude": 106.816666,
  "speed": 45.5,
  "accuracy": 12.5,
  "heading": 180.0,
  "altitude": 25.5,
  "recorded_at_utc": "2024-01-15T10:30:45.123Z",
  "timezone": "Asia/Jakarta",
  "recorded_at_local": "2024-01-15T17:30:45.123+07:00"
}
```

### 5. Backend Processing

Backend menerima request dan:
1. Parse timestamp strings ke `time.Time`
2. Buat entity `Location`
3. Simpan ke database PostgreSQL
4. Broadcast update via WebSocket ke dashboard

### 6. Database Schema

Table: `locations`
```sql
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    speed DECIMAL(5,2),
    accuracy DECIMAL(6,2),
    heading DECIMAL(5,2),
    altitude DECIMAL(7,2),
    recorded_at_utc TIMESTAMP NOT NULL,
    timezone VARCHAR(50),
    recorded_at_local TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Semua Column Terisi

### Required Fields (NOT NULL)
✅ `user_id` - Dari PreferencesManager (login session)
✅ `latitude` - Dari location.latitude
✅ `longitude` - Dari location.longitude
✅ `recorded_at_utc` - Dari location.time → UTC
✅ `timezone` - Dari ZoneId.systemDefault()
✅ `recorded_at_local` - Dari location.time → Local Time

### Optional Fields (Nullable)
✅ `speed` - Dari location.speed (selalu ada jika GPS bergerak)
✅ `accuracy` - Dari location.accuracy (selalu ada dari GPS)
✅ `heading` - Dari location.bearing (null jika tidak bergerak)
✅ `altitude` - Dari location.altitude (null jika GPS tidak support)

### Auto-Generated
✅ `id` - Auto increment (PRIMARY KEY)
✅ `created_at` - Auto timestamp saat insert

## Cara Menggunakan

### 1. Login User
User harus login terlebih dahulu agar `user_id` tersimpan di SharedPreferences.

### 2. Start Tracking
```kotlin
// Di Activity/Fragment
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_START_TRACKING
}
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}
```

### 3. Stop Tracking
```kotlin
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_STOP_TRACKING
}
context.startService(intent)
```

### 4. Monitor Status
```kotlin
// Bind to service
val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as LocationTrackingService.LocationBinder
        val trackingService = binder.getService()
        
        // Observe tracking state
        lifecycleScope.launch {
            trackingService.isTracking.collect { isTracking ->
                // Update UI
            }
        }
        
        // Observe location updates
        lifecycleScope.launch {
            trackingService.locationFlow.collect { location ->
                // Update UI with location
            }
        }
    }
    
    override fun onServiceDisconnected(name: ComponentName?) {}
}

bindService(
    Intent(this, LocationTrackingService::class.java),
    serviceConnection,
    Context.BIND_AUTO_CREATE
)
```

## Error Handling

### Mobile Side
- **No User ID**: Log warning, skip sending
- **Network Error**: Log error, retry pada update berikutnya
- **API Error**: Log error dengan response code

### Backend Side
- **Invalid Request**: Return 400 Bad Request
- **Database Error**: Return 500 Internal Server Error
- **Success**: Return 201 Created dengan location data

## Throttling & Performance

### Mobile
- GPS Update: Setiap 5 detik
- Backend Send: Setiap 30 detik (throttled)
- Minimum Movement: 5 meter untuk hitung jarak

### Backend
- WebSocket broadcast untuk real-time update
- Index pada `user_id` dan `recorded_at_utc` untuk query cepat

## Testing

### Test Manual
1. Login ke app
2. Start tracking
3. Berjalan/berkendara
4. Check logs: `adb logcat | grep LocationTrackingService`
5. Check backend logs
6. Check database: `SELECT * FROM locations ORDER BY created_at DESC LIMIT 10;`

### Expected Logs
```
D/LocationTrackingService: Sending location: TrackLocationRequest(...)
D/LocationRepository: Location sent successfully to backend
```

## Troubleshooting

### Location tidak terkirim
1. ✅ Check user sudah login (user_id tersimpan)
2. ✅ Check permission GPS granted
3. ✅ Check internet connection
4. ✅ Check backend API running
5. ✅ Check API URL di RetrofitClient

### Data tidak lengkap
- `heading` null → Normal jika device tidak bergerak
- `altitude` null → Normal jika GPS tidak support altitude
- `speed` 0 → Normal jika device diam

### Backend error
- Check backend logs: `docker logs <container_name>`
- Check database connection
- Check table schema match dengan entity

## Dependencies

### Mobile
```gradle
// Location Services
implementation 'com.google.android.gms:play-services-location:21.0.1'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Backend
```go
// Fiber web framework
github.com/gofiber/fiber/v2

// GORM
gorm.io/gorm
gorm.io/driver/postgres
```

## Security

### Mobile
- Token disimpan di DataStore (encrypted)
- HTTPS untuk semua API calls
- Location permission required

### Backend
- JWT authentication
- CORS configured
- SQL injection prevention (GORM)
- Input validation

## Future Improvements

1. **Offline Support**: Queue locations saat offline, kirim saat online
2. **Battery Optimization**: Adaptive update interval based on battery level
3. **Geofencing**: Alert saat masuk/keluar area tertentu
4. **Route Optimization**: Suggest optimal route based on traffic
5. **Analytics**: Dashboard untuk analisis perjalanan driver

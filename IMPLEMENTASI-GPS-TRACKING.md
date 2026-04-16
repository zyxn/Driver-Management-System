# Implementasi GPS Tracking - Mobile ke Backend

## ✅ Yang Sudah Dibuat

### 1. Backend API (Sudah Ada)
- ✅ Endpoint: `POST /locations/track`
- ✅ Handler: `LocationHandler.TrackLocation`
- ✅ UseCase: `LocationUseCase.TrackLocation`
- ✅ Repository: `LocationRepository.Create`
- ✅ Entity: `Location` dengan semua field

### 2. Mobile App - API Layer
**File**: `mobile/app/src/main/java/com/example/driver_management_system/data/remote/ApiService.kt`
- ✅ Tambah endpoint `trackLocation()`
- ✅ Tambah DTO `TrackLocationRequest`
- ✅ Tambah DTO `TrackLocationResponse`

### 3. Mobile App - Repository Layer
**File**: `mobile/app/src/main/java/com/example/driver_management_system/domain/repository/LocationRepository.kt`
- ✅ Interface `LocationRepository`

**File**: `mobile/app/src/main/java/com/example/driver_management_system/data/repository/LocationRepositoryImpl.kt`
- ✅ Implementasi `sendLocationUpdate()`
- ✅ Konversi timestamp ke UTC dan Local
- ✅ Konversi speed m/s ke km/h
- ✅ Handle nullable fields (heading, altitude)

### 4. Mobile App - Use Case Layer
**File**: `mobile/app/src/main/java/com/example/driver_management_system/domain/usecase/SendLocationUseCase.kt`
- ✅ Use case untuk send location

### 5. Mobile App - Service Layer
**File**: `mobile/app/src/main/java/com/example/driver_management_system/data/service/LocationTrackingService.kt`
- ✅ Integrasi dengan `SendLocationUseCase`
- ✅ Throttling: kirim setiap 10 detik
- ✅ Coroutine scope untuk background operation
- ✅ Error handling dan logging

### 6. Mobile App - Preferences
**File**: `mobile/app/src/main/java/com/example/driver_management_system/data/local/PreferencesManager.kt`
- ✅ Method `getUserId()` synchronous
- ✅ Method `saveUserIdSync()` untuk save ke SharedPreferences

**File**: `mobile/app/src/main/java/com/example/driver_management_system/data/repository/AuthRepositoryImpl.kt`
- ✅ Save userId synchronously saat login

### 7. Mobile App - UI
**File**: `mobile/app/src/main/java/com/example/driver_management_system/presentation/tracking/TrackingScreen.kt`
- ✅ UI untuk start/stop tracking
- ✅ Display speed, distance, location
- ✅ Permission handling
- ✅ Service binding

### 8. Dokumentasi
**File**: `mobile/README-GPS-TRACKING.md`
- ✅ Arsitektur lengkap
- ✅ Data flow
- ✅ Konversi data
- ✅ Database schema
- ✅ Cara penggunaan
- ✅ Troubleshooting

## 📊 Mapping Data: Mobile → Backend

| Mobile (Android Location) | Backend (Location Entity) | Konversi |
|---------------------------|---------------------------|----------|
| `location.latitude` | `Latitude` | Direct |
| `location.longitude` | `Longitude` | Direct |
| `location.speed` | `Speed` | m/s → km/h (× 3.6) |
| `location.accuracy` | `Accuracy` | Direct (meters) |
| `location.bearing` | `Heading` | Direct (nullable) |
| `location.altitude` | `Altitude` | Direct (nullable) |
| `location.time` | `RecordedAtUTC` | Millis → RFC3339 UTC |
| `location.time` | `RecordedAtLocal` | Millis → RFC3339 Local |
| `ZoneId.systemDefault()` | `Timezone` | String (e.g., "Asia/Jakarta") |
| `PreferencesManager.getUserId()` | `UserID` | Direct |
| - | `ID` | Auto-generated |
| - | `CreatedAt` | Auto-timestamp |

## 🔄 Flow Lengkap

```
1. User Login
   └─> userId disimpan di SharedPreferences (sync)

2. User Start Tracking
   └─> LocationTrackingService.startTracking()
       └─> FusedLocationProvider mulai update (setiap 5 detik)

3. GPS Update Received
   └─> LocationCallback.onLocationResult()
       ├─> Update UI (speed, distance)
       └─> sendLocationToBackend() [throttled 30 detik]
           └─> SendLocationUseCase
               └─> LocationRepository.sendLocationUpdate()
                   ├─> Konversi timestamp
                   ├─> Konversi speed
                   └─> POST /locations/track
                       └─> Backend save ke database
                           └─> Broadcast via WebSocket

4. User Stop Tracking
   └─> LocationTrackingService.stopTracking()
       └─> Stop GPS updates
```

## 🎯 Semua Column Database Terisi

### Required (NOT NULL) ✅
- `user_id` → Dari login session
- `latitude` → Dari GPS
- `longitude` → Dari GPS
- `recorded_at_utc` → Dari GPS timestamp
- `timezone` → Dari system timezone
- `recorded_at_local` → Dari GPS timestamp + timezone

### Optional (Nullable) ✅
- `speed` → Dari GPS (0 jika diam)
- `accuracy` → Dari GPS (selalu ada)
- `heading` → Dari GPS (null jika tidak bergerak)
- `altitude` → Dari GPS (null jika tidak support)

### Auto-Generated ✅
- `id` → Auto increment
- `created_at` → Auto timestamp

## 🚀 Cara Menggunakan

### 1. Pastikan User Sudah Login
```kotlin
// User login → userId tersimpan otomatis
```

### 2. Tambahkan TrackingScreen ke Navigation
```kotlin
// Di MainActivity atau Navigation setup
composable("tracking") {
    TrackingScreen()
}
```

### 3. Start Tracking
- Buka TrackingScreen
- Tap tombol hijau (Play)
- Grant permissions (Location, Notification)
- Tracking dimulai

### 4. Monitor
- Speed real-time
- Total distance
- Current location
- Backend logs

### 5. Stop Tracking
- Tap tombol merah (Stop)
- Service berhenti
- Data tersimpan di database

## 🔍 Testing

### Check Logs Mobile
```bash
adb logcat | grep -E "LocationTrackingService|LocationRepository"
```

Expected output:
```
D/LocationTrackingService: Sending location: TrackLocationRequest(...)
D/LocationRepository: Location sent successfully to backend
```

### Check Backend
```bash
# Check API logs
docker logs <backend-container>

# Check database
psql -U postgres -d driver_management
SELECT * FROM locations ORDER BY created_at DESC LIMIT 10;
```

### Check Dashboard
- Buka frontend dashboard
- Lihat real-time location update via WebSocket

## ⚠️ Troubleshooting

### Location tidak terkirim
1. Check user sudah login
2. Check permission granted
3. Check internet connection
4. Check backend running
5. Check API URL di `RetrofitClient`

### Data tidak lengkap
- `heading` null → Normal jika tidak bergerak
- `altitude` null → Normal jika GPS tidak support
- `speed` 0 → Normal jika diam

### Service mati
- Check battery optimization disabled
- Check notification permission granted
- Service akan auto-restart (START_STICKY)

## 📦 Dependencies yang Dibutuhkan

Pastikan di `build.gradle.kts`:
```kotlin
// Location Services
implementation("com.google.android.gms:play-services-location:21.0.1")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## ✨ Fitur

- ✅ Real-time GPS tracking
- ✅ Automatic send to backend (every 30 seconds)
- ✅ Foreground service (tidak bisa di-dismiss)
- ✅ Battery efficient (throttled updates)
- ✅ Offline resilient (akan retry)
- ✅ Auto-restart after app killed
- ✅ Distance calculation
- ✅ Speed monitoring
- ✅ WebSocket broadcast ke dashboard

## 🎉 Selesai!

Sistem GPS tracking sudah lengkap dan siap digunakan. Semua column di table `locations` akan terisi dengan data yang sesuai dari mobile app.

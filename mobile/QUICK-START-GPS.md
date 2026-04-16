# Quick Start - GPS Tracking

## 🚀 Setup Cepat (5 Menit)

### 1. Pastikan Dependencies Ada
File: `mobile/app/build.gradle.kts`
```kotlin
dependencies {
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Networking (sudah ada)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines (sudah ada)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 2. Tambahkan Permissions
File: `mobile/app/src/main/AndroidManifest.xml`
```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <application>
        <!-- Service (sudah ada) -->
        <service
            android:name=".data.service.LocationTrackingService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="location" />
    </application>
</manifest>
```

### 3. Tambahkan TrackingScreen ke Navigation
File: `mobile/app/src/main/java/com/example/driver_management_system/MainActivity.kt`

Tambahkan route:
```kotlin
composable("tracking") {
    TrackingScreen()
}
```

Tambahkan menu item untuk navigate ke tracking:
```kotlin
// Di menu atau navigation
navController.navigate("tracking")
```

### 4. Test!

#### A. Login dulu
```kotlin
// User harus login agar userId tersimpan
```

#### B. Buka Tracking Screen
```kotlin
// Navigate ke "tracking" route
```

#### C. Start Tracking
- Tap tombol hijau (Play)
- Grant permissions
- Lihat speed, distance, location update

#### D. Check Logs
```bash
adb logcat | grep LocationTrackingService
```

Expected:
```
D/LocationTrackingService: Sending location: TrackLocationRequest(...)
D/LocationRepository: Location sent successfully to backend
```

#### E. Check Backend
```bash
# Check database
docker exec -it <postgres-container> psql -U postgres -d driver_management

SELECT id, user_id, latitude, longitude, speed, 
       recorded_at_utc, timezone, created_at 
FROM locations 
ORDER BY created_at DESC 
LIMIT 5;
```

Expected output:
```
 id | user_id | latitude  | longitude  | speed | recorded_at_utc      | timezone      | created_at
----+---------+-----------+------------+-------+----------------------+---------------+------------
  1 |       1 | -6.200000 | 106.816666 | 45.50 | 2024-01-15 10:30:45  | Asia/Jakarta  | 2024-01-15...
```

## 📱 Cara Pakai di Kode

### Start Tracking Programmatically
```kotlin
import android.content.Intent
import android.os.Build
import com.example.driver_management_system.data.service.LocationTrackingService

fun startTracking(context: Context) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_START_TRACKING
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
```

### Stop Tracking
```kotlin
fun stopTracking(context: Context) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_STOP_TRACKING
    }
    context.startService(intent)
}
```

### Monitor Status
```kotlin
// Bind to service
val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as LocationTrackingService.LocationBinder
        val trackingService = binder.getService()
        
        // Observe tracking state
        lifecycleScope.launch {
            trackingService.isTracking.collect { isTracking ->
                println("Tracking: $isTracking")
            }
        }
        
        // Observe location
        lifecycleScope.launch {
            trackingService.locationFlow.collect { location ->
                location?.let {
                    println("Lat: ${it.latitude}, Lng: ${it.longitude}")
                    println("Speed: ${it.speed * 3.6} km/h")
                }
            }
        }
        
        // Observe distance
        lifecycleScope.launch {
            trackingService.totalDistance.collect { distance ->
                println("Distance: ${distance / 1000} km")
            }
        }
    }
    
    override fun onServiceDisconnected(name: ComponentName?) {}
}

// Bind
context.bindService(
    Intent(context, LocationTrackingService::class.java),
    serviceConnection,
    Context.BIND_AUTO_CREATE
)
```

## 🔧 Konfigurasi

### Ubah Interval Update
File: `LocationTrackingService.kt`
```kotlin
companion object {
    private const val UPDATE_INTERVAL_MS = 5000L      // GPS update
    private const val SEND_INTERVAL_MS = 30000L       // Send ke backend
}
```

### Ubah Minimum Movement
File: `LocationTrackingService.kt`
```kotlin
private fun setupLocationCallback() {
    locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // ...
            val distance = last.distanceTo(location)
            if (distance > 5) { // Ubah nilai ini (dalam meter)
                // ...
            }
        }
    }
}
```

## 🐛 Debug

### Enable Verbose Logging
```bash
adb shell setprop log.tag.LocationTrackingService VERBOSE
adb shell setprop log.tag.LocationRepository VERBOSE
adb logcat -s LocationTrackingService:V LocationRepository:V
```

### Mock Location (Testing)
```kotlin
// Enable mock location di Developer Options
// Gunakan app seperti "Fake GPS Location" untuk testing
```

### Check Service Running
```bash
adb shell dumpsys activity services | grep LocationTrackingService
```

## ✅ Checklist

- [ ] Dependencies installed
- [ ] Permissions added to manifest
- [ ] Service declared in manifest
- [ ] TrackingScreen added to navigation
- [ ] User can login (userId saved)
- [ ] Can start tracking
- [ ] Can see location updates
- [ ] Backend receives data
- [ ] Database has records
- [ ] Can stop tracking

## 🎯 Next Steps

1. **Offline Support**: Queue locations saat offline
2. **Battery Optimization**: Adaptive interval
3. **Geofencing**: Alert masuk/keluar area
4. **Route History**: Tampilkan rute di map
5. **Analytics**: Dashboard perjalanan

## 📚 Dokumentasi Lengkap

Lihat: `mobile/README-GPS-TRACKING.md` untuk detail lengkap.

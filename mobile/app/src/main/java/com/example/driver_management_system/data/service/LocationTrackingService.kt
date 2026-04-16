package com.example.driver_management_system.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.driver_management_system.MainActivity
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.data.repository.LocationRepositoryImpl
import com.example.driver_management_system.domain.usecase.SendLocationUseCase
import com.example.driver_management_system.utils.MockLocationDetector
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences
    private lateinit var preferencesManager: PreferencesManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var trackingStartTimeMs: Long = 0L
    
    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Use case for sending location to backend
    private lateinit var sendLocationUseCase: SendLocationUseCase

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _totalDistance = MutableStateFlow(0f)
    val totalDistance: StateFlow<Float> = _totalDistance.asStateFlow()

    private var lastLocation: Location? = null
    private var lastSentTime: Long = 0L

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferencesManager = PreferencesManager(this)
        
        // Initialize location repository and use case
        val apiService = RetrofitClient.apiService
        val locationRepository = LocationRepositoryImpl(apiService)
        sendLocationUseCase = SendLocationUseCase(locationRepository)
        
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
            else -> {
                // Service restarted by system (START_STICKY) or by BootReceiver
                // Check persisted state to decide if we should resume
                val wasTracking = prefs.getBoolean(PREF_IS_TRACKING, false)
                if (wasTracking) {
                    // Restore persisted distance
                    val savedDistance = prefs.getFloat(PREF_TOTAL_DISTANCE, 0f)
                    _totalDistance.value = savedDistance
                    
                    val savedStartTime = prefs.getLong(PREF_START_TIME, 0L)
                    trackingStartTimeMs = if (savedStartTime > 0) savedStartTime else SystemClock.elapsedRealtime()
                    
                    startTracking(isResuming = true)
                }
            }
        }
        return START_STICKY // Service will be restarted if killed by system
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for regular tracking (Low importance)
            val trackingChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW // LOW = no sound, but always visible
            ).apply {
                description = "GPS lokasi driver sedang aktif."
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            // Channel for alerts (High importance)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Location Alerts",
                NotificationManager.IMPORTANCE_HIGH // HIGH = makes sound and pops up
            ).apply {
                description = "Notifikasi peringatan sistem GPS."
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500) // Vibrate pattern
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Check for mock location
                    val mockStatus = MockLocationDetector.checkMockLocation(this@LocationTrackingService, location)
                    
                    when (mockStatus) {
                        MockLocationDetector.MockLocationStatus.BOTH_DETECTED,
                        MockLocationDetector.MockLocationStatus.LOCATION_MOCKED -> {
                            // Location is fake - reject it
                            Log.w(TAG, "Mock location detected! Rejecting location update.")
                            showMockLocationWarning()
                            return
                        }
                        MockLocationDetector.MockLocationStatus.SETTINGS_ENABLED -> {
                            // Settings enabled but location might be real - log warning
                            Log.w(TAG, "Mock location enabled in settings, but location appears genuine")
                        }
                        MockLocationDetector.MockLocationStatus.GENUINE -> {
                            // Location is genuine - proceed normally
                        }
                    }
                    
                    _locationFlow.value = location
                    
                    // Calculate distance
                    lastLocation?.let { last ->
                        val distance = last.distanceTo(location)
                        if (distance > 5) { // Only count if moved more than 5 meters
                            _totalDistance.value += distance
                            // Persist distance so we don't lose it on restart
                            prefs.edit().putFloat(PREF_TOTAL_DISTANCE, _totalDistance.value).apply()
                        }
                    }
                    lastLocation = location
                    
                    updateNotification(location)
                    
                    // Send location to backend
                    sendLocationToBackend(location)
                }
            }
        }
    }
    
    private fun showMockLocationWarning() {
        // Update notification to show warning using the ALERT channel
        val warningNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("⚠️ Mock Location Terdeteksi")
            .setContentText("Matikan Mock Location di Developer Options untuk melanjutkan tracking")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, warningNotification)
    }
    
    private fun sendLocationToBackend(location: Location) {
        // Get user ID and role from preferences
        val userId = preferencesManager.getUserId()
        val userRole = preferencesManager.getUserRole()
        
        if (userId == null) {
            Log.w(TAG, "User ID not found, skipping location send")
            return
        }
        
        // Only send location if user is a driver
        if (!userRole.equals("driver", ignoreCase = true)) {
            Log.w(TAG, "User is not a driver (role: $userRole), skipping location send")
            return
        }
        
        // Throttle: only send every SEND_INTERVAL_MS
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSentTime < SEND_INTERVAL_MS) {
            return
        }
        lastSentTime = currentTime
        
        // Send location in background
        serviceScope.launch {
            try {
                val result = sendLocationUseCase(userId, location)
                result.onSuccess {
                    Log.d(TAG, "Location sent successfully to backend")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to send location to backend: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending location to backend", e)
            }
        }
    }

    fun startTracking(isResuming: Boolean = false) {
        if (_isTracking.value && !isResuming) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LocationTrackingService::WakeLock"
        ).apply {
            acquire(24 * 60 * 60 * 1000L) // 24 hours max (full shift)
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
            setWaitForAccurateLocation(false)
            setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        _isTracking.value = true
        
        if (!isResuming) {
            _totalDistance.value = 0f
            lastLocation = null
            trackingStartTimeMs = SystemClock.elapsedRealtime()
        }

        // Persist tracking state
        prefs.edit()
            .putBoolean(PREF_IS_TRACKING, true)
            .putLong(PREF_START_TIME, trackingStartTimeMs)
            .apply()

        // Start foreground IMMEDIATELY - this is critical
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun stopTracking() {
        if (!_isTracking.value) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isTracking.value = false
        
        // Clear persisted state
        prefs.edit()
            .putBoolean(PREF_IS_TRACKING, false)
            .putFloat(PREF_TOTAL_DISTANCE, 0f)
            .putLong(PREF_START_TIME, 0L)
            .apply()
        
        // Release wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(location: Location? = null): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Calculate elapsed time
        val elapsedMs = if (trackingStartTimeMs > 0) {
            SystemClock.elapsedRealtime() - trackingStartTimeMs
        } else 0L
        val elapsedSeconds = (elapsedMs / 1000).toInt()
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val distanceKm = _totalDistance.value / 1000
        
        val contentText = if (location != null) {
            val speed = location.speed * 3.6f
            "⏱ $timeStr • 📏 %.2f km • 🚗 %.0f km/h".format(distanceKm, speed)
        } else {
            "⏱ $timeStr • Mengakuisisi sinyal GPS..."
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Tracking Lokasi Aktif")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            // === STICKY NOTIFICATION - CANNOT BE DISMISSED ===
            .setOngoing(true)              // Mark as ongoing - cannot be swiped away
            .setAutoCancel(false)           // Cannot be cancelled by tapping
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low = no sound, but visible
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // Show elapsed time like Strava
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - elapsedMs)
            .setChronometerCountDown(false)
            // Prevent clearing
            .setDeleteIntent(null)
            .build()

        // Extra flags to make it truly undismissable
        notification.flags = notification.flags or
                Notification.FLAG_NO_CLEAR or      // Cannot be cleared
                Notification.FLAG_ONGOING_EVENT or  // Ongoing event
                Notification.FLAG_FOREGROUND_SERVICE // Foreground service flag

        return notification
    }

    private fun updateNotification(location: Location) {
        if (_isTracking.value) {
            val notification = createNotification(location)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        // Remove location updates to prevent leaks
        if (::locationCallback.isInitialized && ::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        
        // Release wake lock if held
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        // If we were tracking, the system killed us. 
        // START_STICKY + onTaskRemoved will handle restart.
        // The persisted state (SharedPreferences) will tell us to resume on restart.
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        // App was swiped away from recent apps
        // Only restart if we were actively tracking
        val wasTracking = prefs.getBoolean(PREF_IS_TRACKING, false)
        if (wasTracking) {
            // Re-start the service immediately
            val restartServiceIntent = Intent(applicationContext, LocationTrackingService::class.java).apply {
                setPackage(packageName)
                // No action = will trigger the "else" branch in onStartCommand
                // which checks SharedPreferences and resumes tracking
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartServiceIntent)
            } else {
                applicationContext.startService(restartServiceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "LocationTrackingService"
        const val CHANNEL_ID = "location_tracking_channel"
        const val ALERT_CHANNEL_ID = "location_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        
        private const val UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val FASTEST_UPDATE_INTERVAL_MS = 2000L // 2 seconds
        private const val SEND_INTERVAL_MS = 30000L // Send to backend every 30 seconds
        
        // SharedPreferences for persisting tracking state across process death
        private const val PREFS_NAME = "location_tracking_prefs"
        private const val PREF_IS_TRACKING = "is_tracking"
        private const val PREF_TOTAL_DISTANCE = "total_distance"
        private const val PREF_START_TIME = "start_time_elapsed"
    }
}

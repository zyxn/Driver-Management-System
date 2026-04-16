package com.example.driver_management_system

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.data.manager.FCMManager
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.data.repository.AuthRepositoryImpl
import com.example.driver_management_system.data.service.LocationTrackingService
import com.example.driver_management_system.domain.usecase.LoginUseCase
import com.example.driver_management_system.presentation.home.HomeScreen
import com.example.driver_management_system.presentation.login.LoginScreen
import com.example.driver_management_system.presentation.login.LoginViewModel
import com.example.driver_management_system.ui.theme.DriverManagementSystemTheme
import com.example.driver_management_system.utils.AuthEvent
import com.example.driver_management_system.utils.AuthEventBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Apapun hasilnya, kita lanjut ke battery optimization. 
        // Idealnya beritahu user kalau background location penting.
        requestBatteryOptimizationExemption()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            checkAndRequestBackgroundLocation()
        }
    }
    
    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Di Android 11+ (API 30+), kita harus mengarahkan user ke setting secara eksplisit
                // Tapi launch() default akan memunculkan dialog "Allow all the time" 
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return
            }
        }
        requestBatteryOptimizationExemption()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Simple DI - untuk production gunakan Hilt/Koin
        val repository = AuthRepositoryImpl(applicationContext)
        val loginUseCase = LoginUseCase(repository)
        val viewModel = LoginViewModel(loginUseCase)
        val preferencesManager = PreferencesManager(applicationContext)
        
        setContent {
            DriverManagementSystemTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        preferencesManager = preferencesManager,
                        onLoginSuccess = { role ->
                            // Only start GPS tracking if user is a driver
                            if (role.equals("driver", ignoreCase = true)) {
                                requestLocationPermissionAndStartTracking()
                            }
                        },
                        onLogoutAction = {
                            stopTrackingService()
                        }
                    )
                }
            }
        }
    }
    
    private fun requestLocationPermissionAndStartTracking() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check location permissions
        if (!hasLocationPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isEmpty()) {
            // All foreground permissions granted, now check background
            checkAndRequestBackgroundLocation()
        } else {
            // Request missing permissions
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // Request to disable battery optimization
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback if direct request not available
                    val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(settingsIntent)
                }
            }
        }
        
        // Start tracking service
        startTrackingService()
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startTrackingService() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopTrackingService() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        startService(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Service akan tetap berjalan di background
    }
}

// Helper function to send FCM token to backend
private fun sendFCMTokenToBackend(context: android.content.Context, preferencesManager: PreferencesManager) {
    kotlinx.coroutines.MainScope().launch {
        try {
            val apiService = RetrofitClient.getAuthenticatedApiService(context)
            val fcmManager = FCMManager(
                context = context,
                apiService = apiService
            )
            
            // Get FCM token and send to backend
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    fcmManager.sendTokenToBackend(token)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MainActivity", "Failed to get FCM token: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to send FCM token: ${e.message}")
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: LoginViewModel,
    preferencesManager: PreferencesManager,
    onLoginSuccess: (role: String) -> Unit,
    onLogoutAction: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) } // null = checking
    var username by remember { mutableStateOf("") }
    
    // Check existing token on app start
    LaunchedEffect(Unit) {
        val token = preferencesManager.token.first()
        val savedUsername = preferencesManager.fullName.first() 
            ?: preferencesManager.username.first()
        val savedRole = preferencesManager.role.first()
        
        if (!token.isNullOrEmpty()) {
            // User already logged in
            isLoggedIn = true
            username = savedUsername ?: ""
            
            // Send FCM token to backend
            sendFCMTokenToBackend(context, preferencesManager)
            
            // Auto-start GPS tracking if user is a driver
            if (savedRole.equals("driver", ignoreCase = true)) {
                onLoginSuccess(savedRole ?: "")
            }
        } else {
            // No token, show login
            isLoggedIn = false
        }
    }
    
    // Listen to auth events (token expiration)
    LaunchedEffect(Unit) {
        AuthEventBus.events.collect { event ->
            when (event) {
                is AuthEvent.TokenExpired,
                is AuthEvent.Unauthorized -> {
                    // Token expired, logout user
                    isLoggedIn = false
                    username = ""
                    viewModel.resetState()
                }
            }
        }
    }
    
    // Handle new login success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && isLoggedIn != true) {
            isLoggedIn = true
            username = uiState.user?.fullName ?: uiState.user?.username ?: ""
            val userRole = uiState.user?.role ?: ""
            
            // Send FCM token to backend after successful login
            sendFCMTokenToBackend(context, preferencesManager)
            
            onLoginSuccess(userRole)
        }
    }
    
    when (isLoggedIn) {
        true -> {
            HomeScreen(
                username = username,
                onLogout = {
                    kotlinx.coroutines.MainScope().launch {
                        preferencesManager.clearAll()
                    }
                    // Stop GPS tracking service when logout
                    onLogoutAction()
                    isLoggedIn = false
                    username = ""
                    viewModel.resetState()
                }
            )
        }
        false -> {
            LoginScreen(viewModel = viewModel)
        }
        null -> {
            // Show loading while checking token
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

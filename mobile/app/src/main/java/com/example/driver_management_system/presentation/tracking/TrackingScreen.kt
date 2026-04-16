package com.example.driver_management_system.presentation.tracking

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.driver_management_system.data.service.LocationTrackingService
import kotlinx.coroutines.launch

@Composable
fun TrackingScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var trackingService by remember { mutableStateOf<LocationTrackingService?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(0f) }
    var totalDistance by remember { mutableStateOf(0f) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startTracking(context)
        }
    }
    
    // Service connection
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as LocationTrackingService.LocationBinder
                trackingService = binder.getService()
                
                // Observe tracking state
                scope.launch {
                    trackingService?.isTracking?.collect { tracking ->
                        isTracking = tracking
                    }
                }
                
                // Observe location updates
                scope.launch {
                    trackingService?.locationFlow?.collect { location ->
                        currentLocation = location
                        location?.let {
                            currentSpeed = it.speed * 3.6f // m/s to km/h
                        }
                    }
                }
                
                // Observe distance
                scope.launch {
                    trackingService?.totalDistance?.collect { distance ->
                        totalDistance = distance
                    }
                }
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                trackingService = null
            }
        }
    }
    
    // Bind to service
    DisposableEffect(Unit) {
        context.bindService(
            Intent(context, LocationTrackingService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        
        onDispose {
            context.unbindService(serviceConnection)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                text = if (isTracking) "Tracking Aktif" else "Tracking Tidak Aktif",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 32.dp)
            )
            
            // Stats Cards
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Speed Card
                StatsCard(
                    icon = Icons.Default.Speed,
                    label = "Kecepatan",
                    value = "%.0f".format(currentSpeed),
                    unit = "km/h",
                    color = Color(0xFF10B981)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Distance Card
                StatsCard(
                    icon = Icons.Default.Route,
                    label = "Jarak Tempuh",
                    value = "%.2f".format(totalDistance / 1000),
                    unit = "km",
                    color = Color(0xFF8B5CF6)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location Card
                if (currentLocation != null) {
                    LocationCard(
                        latitude = currentLocation!!.latitude,
                        longitude = currentLocation!!.longitude,
                        accuracy = currentLocation!!.accuracy
                    )
                }
            }
            
            // Control Button
            Box(
                modifier = Modifier
                    .padding(bottom = 48.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isTracking) {
                            stopTracking(context)
                        } else {
                            checkPermissionsAndStart(context, permissionLauncher)
                        }
                    },
                    modifier = Modifier
                        .size(80.dp),
                    containerColor = if (isTracking) Color(0xFFEF4444) else Color(0xFF10B981),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationCard(
    latitude: Double,
    longitude: Double,
    accuracy: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lokasi Saat Ini",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Lat: %.6f".format(latitude),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "Lng: %.6f".format(longitude),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "Akurasi: %.1f m".format(accuracy),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

private fun checkPermissionsAndStart(
    context: Context,
    permissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    val allGranted = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    
    if (allGranted) {
        startTracking(context)
    } else {
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

private fun startTracking(context: Context) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_START_TRACKING
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopTracking(context: Context) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_STOP_TRACKING
    }
    context.startService(intent)
}

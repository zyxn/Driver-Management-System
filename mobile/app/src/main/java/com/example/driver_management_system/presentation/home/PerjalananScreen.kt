package com.example.driver_management_system.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.driver_management_system.data.service.RoutingService
import com.example.driver_management_system.domain.model.TripHistory
import com.example.driver_management_system.domain.model.TripType
import com.example.driver_management_system.ui.theme.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerjalananScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember { PerjalananViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedTrip by remember { mutableStateOf<TripHistory?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // DatePicker state - use current date
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    val routingService = remember { RoutingService() }
    
    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.loadTripsForDate(uiState.selectedDate)
    }
    
    // Convert selected date from DatePicker to string format
    val selectedDateString = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = millis
            }
            String.format(
                "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        } ?: uiState.selectedDate
    }
    
    // Update map when trips or location history change
    LaunchedEffect(uiState.trips, uiState.locationHistory, mapView) {
        if (mapView == null) return@LaunchedEffect
        
        // Debug logging
        android.util.Log.d("PerjalananScreen", "Trips count: ${uiState.trips.size}")
        android.util.Log.d("PerjalananScreen", "Location history count: ${uiState.locationHistory.size}")
        
        mapView?.let { map ->
            // Clear existing overlays
            map.overlays.clear()
            
            // Add GPS track polyline if available
            if (uiState.locationHistory.isNotEmpty()) {
                android.util.Log.d("PerjalananScreen", "Adding polyline with ${uiState.locationHistory.size} points")
                val polyline = Polyline().apply {
                    setPoints(uiState.locationHistory)
                    outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                    outlinePaint.strokeWidth = 8f
                }
                map.overlays.add(0, polyline)
            } else {
                android.util.Log.w("PerjalananScreen", "No location history to display")
            }
            
            // Add numbered markers for trips/reports
            uiState.trips.forEachIndexed { index, trip ->
                val marker = Marker(map).apply {
                    position = GeoPoint(trip.latitude, trip.longitude)
                    title = "${index + 1}. ${trip.location}"
                    snippet = trip.timestamp
                    
                    // Create custom marker icon with number
                    icon = createNumberedMarkerIcon(context, index + 1, trip.type)
                    
                    setOnMarkerClickListener { _, _ ->
                        selectedTrip = trip
                        showBottomSheet = true
                        true
                    }
                }
                map.overlays.add(marker)
            }
            
            // Center map on first point if available
            if (uiState.trips.isNotEmpty()) {
                map.controller.setCenter(GeoPoint(uiState.trips[0].latitude, uiState.trips[0].longitude))
                map.controller.setZoom(12.0)
            } else if (uiState.locationHistory.isNotEmpty()) {
                map.controller.setCenter(uiState.locationHistory[0])
                map.controller.setZoom(12.0)
            }
            
            map.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    
                    // Set center to Jakarta
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(-6.2088, 106.8456))
                    
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Date Filter Button
        FloatingActionButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = PrimaryBlue
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Filter Tanggal",
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
        
        // Selected Date Card
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Tanggal",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = formatDate(uiState.selectedDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (uiState.trips.isNotEmpty()) {
                    Text(
                        text = "${uiState.trips.size} perjalanan",
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                } else if (uiState.isLoading) {
                    Text(
                        text = "Memuat...",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = "Tidak ada perjalanan",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                // Show GPS points count
                if (uiState.locationHistory.isNotEmpty()) {
                    Text(
                        text = "${uiState.locationHistory.size} GPS points",
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                    )
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memuat data...",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
        
        // Error message
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFEBEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.error ?: "Error",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    viewModel.setSelectedDate(selectedDateString)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PrimaryBlue,
                    todayContentColor = PrimaryBlue,
                    todayDateBorderColor = PrimaryBlue
                )
            )
        }
    }

    // Bottom Sheet for trip details
    if (showBottomSheet && selectedTrip != null) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = CardBackground
        ) {
            TripDetailContent(
                trip = selectedTrip!!,
                onViewPhoto = {
                    showImageViewer = true
                }
            )
        }
    }
    
    // Image Viewer as separate fullscreen overlay
    if (showImageViewer && selectedTrip?.photoUrl != null) {
        com.example.driver_management_system.ui.components.ImageViewerDialog(
            imageUrl = selectedTrip!!.photoUrl!!,
            title = "${selectedTrip!!.type.displayName} - ${selectedTrip!!.location}",
            onDismiss = { showImageViewer = false }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

fun formatDate(dateString: String): String {
    // Format: "2024-01-15" -> "15 Januari 2024"
    val parts = dateString.split("-")
    if (parts.size != 3) return dateString
    
    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { if (it in 1..12) months[it - 1] else parts[1] } ?: parts[1]
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    
    return "$day $month $year"
}

@Composable
fun TripDetailContent(
    trip: TripHistory,
    onViewPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getTripTypeColor(trip.type).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTripTypeIcon(trip.type),
                    contentDescription = null,
                    tint = getTripTypeColor(trip.type),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = trip.type.displayName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = trip.location,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Details
        DetailRow(
            icon = Icons.Default.AccessTime,
            label = "Waktu",
            value = trip.timestamp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        DetailRow(
            icon = Icons.Default.LocationOn,
            label = "Alamat",
            value = trip.address
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        DetailRow(
            icon = Icons.Default.Description,
            label = "Catatan",
            value = trip.notes
        )
        
        if (trip.signature != null) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                icon = Icons.Default.CheckCircle,
                label = "Status",
                value = "Tanda tangan diterima"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewPhoto,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = trip.photoUrl != null
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foto Bukti")
            }
            
            Button(
                onClick = { /* View signature */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = trip.signature != null
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tanda Tangan")
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getTripTypeIcon(type: TripType): ImageVector {
    return when (type) {
        TripType.PICKUP -> Icons.Default.LocalShipping
        TripType.DELIVERY -> Icons.Default.Inventory
        TripType.REST -> Icons.Default.LocalCafe
        TripType.REFUEL -> Icons.Default.LocalGasStation
        TripType.CHECKPOINT -> Icons.Default.Flag
    }
}

fun getTripTypeColor(type: TripType): androidx.compose.ui.graphics.Color {
    return when (type) {
        TripType.PICKUP -> PrimaryBlue
        TripType.DELIVERY -> SuccessGreen
        TripType.REST -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        TripType.REFUEL -> androidx.compose.ui.graphics.Color(0xFFF44336)
        TripType.CHECKPOINT -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
    }
}

// Create custom marker icon with number
fun createNumberedMarkerIcon(
    context: android.content.Context,
    number: Int,
    tripType: TripType
): android.graphics.drawable.Drawable {
    val size = 80 // Size in pixels
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Get color based on trip type
    val color = when (tripType) {
        TripType.PICKUP -> android.graphics.Color.parseColor("#2196F3")
        TripType.DELIVERY -> android.graphics.Color.parseColor("#4CAF50")
        TripType.REST -> android.graphics.Color.parseColor("#FF9800")
        TripType.REFUEL -> android.graphics.Color.parseColor("#F44336")
        TripType.CHECKPOINT -> android.graphics.Color.parseColor("#9C27B0")
    }
    
    // Draw circle background
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color
        style = android.graphics.Paint.Style.FILL
    }
    
    val centerX = size / 2f
    val centerY = size / 2f
    val radius = size / 2f - 4f
    
    // Draw shadow
    val shadowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.parseColor("#40000000")
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(centerX + 2, centerY + 2, radius, shadowPaint)
    
    // Draw main circle
    canvas.drawCircle(centerX, centerY, radius, paint)
    
    // Draw white border
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(centerX, centerY, radius - 2, borderPaint)
    
    // Draw number text
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        textSize = 36f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    
    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(number.toString(), 0, number.toString().length, textBounds)
    val textY = centerY - textBounds.exactCenterY()
    
    canvas.drawText(number.toString(), centerX, textY, textPaint)
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

package com.example.driver_management_system.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.driver_management_system.domain.model.TripType
import com.example.driver_management_system.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AksiScreen() {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<TripType?>(null) }
    var locationName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
    // Create temp file for camera
    val photoFile = remember {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        File(storageDir, "JPEG_${timeStamp}.jpg")
    }
    
    val photoFileUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = photoFileUri
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, get location
            getCurrentLocation(context) { location, error ->
                currentLocation = location
                locationError = error
                isLoadingLocation = false
            }
        } else {
            locationError = "Izin lokasi diperlukan"
            isLoadingLocation = false
        }
    }
    
    // Get location on screen load
    LaunchedEffect(Unit) {
        isLoadingLocation = true
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation(context) { location, error ->
                    currentLocation = location
                    locationError = error
                    isLoadingLocation = false
                }
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoFileUri)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Tambah Aksi Perjalanan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Catat aktivitas selama perjalanan",
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Location Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentLocation != null) SuccessGreen.copy(alpha = 0.1f) 
                else if (locationError != null) Error.copy(alpha = 0.1f)
                else Info.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                } else {
                    Icon(
                        imageVector = if (currentLocation != null) Icons.Default.GpsFixed 
                        else if (locationError != null) Icons.Default.GpsOff
                        else Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = if (currentLocation != null) SuccessGreen 
                        else if (locationError != null) Error
                        else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoadingLocation) "Mendapatkan lokasi..."
                        else if (currentLocation != null) "Lokasi GPS Aktif"
                        else if (locationError != null) "GPS Tidak Aktif"
                        else "Menunggu GPS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentLocation != null) SuccessGreen 
                        else if (locationError != null) Error
                        else TextSecondary
                    )
                    
                    if (currentLocation != null) {
                        Text(
                            text = "Lat: ${String.format("%.6f", currentLocation!!.latitude)}, " +
                                   "Lng: ${String.format("%.6f", currentLocation!!.longitude)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else if (locationError != null) {
                        Text(
                            text = locationError!!,
                            fontSize = 12.sp,
                            color = Error
                        )
                    }
                }
                
                if (!isLoadingLocation && currentLocation == null) {
                    IconButton(
                        onClick = {
                            isLoadingLocation = true
                            locationError = null
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh lokasi",
                            tint = PrimaryBlue
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Location Name Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Nama Tempat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    placeholder = { Text("Contoh: SPBU Pertamina Tebet") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Photo Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foto Bukti",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Error
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (photoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Foto bukti",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Remove button
                        IconButton(
                            onClick = { photoUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus foto",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 2.dp,
                                color = androidx.compose.ui.graphics.Color.LightGray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                // Langsung buka kamera
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) -> {
                                        cameraLauncher.launch(photoFileUri)
                                    }
                                    else -> {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap untuk ambil foto",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Type Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tipe Aksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = { showTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedType?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Pilih tipe aksi") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showTypeMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        TripType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = getActionTypeIcon(type),
                                            contentDescription = null,
                                            tint = getActionTypeColor(type),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(type.displayName)
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Deskripsi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Tulis catatan atau deskripsi...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit Button
        Button(
            onClick = {
                if (selectedType != null && locationName.isNotBlank() && description.isNotBlank() && currentLocation != null && photoUri != null) {
                    // TODO: Save data with location
                    showSuccessDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedType != null && locationName.isNotBlank() && description.isNotBlank() && currentLocation != null && photoUri != null
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simpan Aksi",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Berhasil!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Aksi perjalanan berhasil disimpan")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        // Reset form
                        selectedType = null
                        locationName = ""
                        description = ""
                        photoUri = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

private fun getActionTypeIcon(type: TripType): ImageVector {
    return when (type) {
        TripType.PICKUP -> Icons.Default.LocalShipping
        TripType.DELIVERY -> Icons.Default.Inventory
        TripType.REST -> Icons.Default.LocalCafe
        TripType.REFUEL -> Icons.Default.LocalGasStation
        TripType.CHECKPOINT -> Icons.Default.Flag
    }
}

private fun getActionTypeColor(type: TripType): androidx.compose.ui.graphics.Color {
    return when (type) {
        TripType.PICKUP -> PrimaryBlue
        TripType.DELIVERY -> SuccessGreen
        TripType.REST -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        TripType.REFUEL -> androidx.compose.ui.graphics.Color(0xFFF44336)
        TripType.CHECKPOINT -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
    }
}

private fun getCurrentLocation(
    context: android.content.Context,
    onResult: (Location?, String?) -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onResult(location, null)
                } else {
                    onResult(null, "Tidak dapat mendapatkan lokasi")
                }
            }.addOnFailureListener { exception ->
                onResult(null, "Error: ${exception.message}")
            }
        } else {
            onResult(null, "Izin lokasi tidak diberikan")
        }
    } catch (e: Exception) {
        onResult(null, "Error: ${e.message}")
    }
}

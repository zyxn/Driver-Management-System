package com.example.driver_management_system.presentation.drivers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.data.repository.DriverRepositoryImpl
import com.example.driver_management_system.domain.usecase.GetAllDriversUseCase
import com.example.driver_management_system.ui.components.DriverCard
import com.example.driver_management_system.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverListScreen(onBack: () -> Unit = {}) {
    // Handle back gesture
    BackHandler(onBack = onBack)
    
    val context = LocalContext.current
    val apiService = RetrofitClient.getAuthenticatedApiService(context)
    val driverRepository = remember { DriverRepositoryImpl(apiService) }
    val getAllDriversUseCase = remember { GetAllDriversUseCase(driverRepository) }
    val viewModel = remember { DriverListViewModel(getAllDriversUseCase, driverRepository) }
    
    val uiState by viewModel.uiState.collectAsState()
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    
    // Get user location
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    location?.let {
                        userLocation = Pair(it.latitude, it.longitude)
                    }
                }
            } catch (e: Exception) {
                // Silently fail if location not available
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daftar Driver",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.drivers.isNotEmpty()) {
                            Text(
                                text = "${uiState.drivers.size} driver",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        },
        containerColor = BackgroundBase
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.drivers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Memuat data driver...",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                uiState.error != null && uiState.drivers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.error ?: "Terjadi kesalahan",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue
                                )
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                
                uiState.drivers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "📋",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tidak ada driver",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Belum ada driver yang terdaftar",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.drivers,
                            key = { it.id }
                        ) { driver ->
                            DriverCard(
                                driver = driver,
                                userLocation = userLocation,
                                onClick = {
                                    // TODO: Navigate to driver detail
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

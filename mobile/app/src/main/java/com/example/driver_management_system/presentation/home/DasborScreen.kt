package com.example.driver_management_system.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.ui.components.MockLocationWarningBanner
import com.example.driver_management_system.ui.components.WeatherCard
import com.example.driver_management_system.ui.theme.*
import com.example.driver_management_system.utils.MockLocationDetector

@Composable
fun DasborScreen(
    username: String,
    onNavigateToDriverList: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { WeatherViewModel(context) }
    val weatherState by viewModel.weatherState.collectAsState()
    
    // Check for mock location
    var isMockLocationEnabled by remember { mutableStateOf(false) }
    var showMockWarning by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadWeather()
        // Check mock location status
        isMockLocationEnabled = MockLocationDetector.isMockLocationEnabled(context)
        showMockWarning = isMockLocationEnabled
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Mock Location Warning Banner
        if (showMockWarning) {
            MockLocationWarningBanner(
                onDismiss = { showMockWarning = false }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Welcome Text
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Selamat Datang,",
                fontSize = 36.sp,
                color = TextSecondary
            )
            Text(
                text = username,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weather Card
        WeatherCard(weatherState = weatherState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Menu",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        MenuGrid(onNavigateToDriverList = onNavigateToDriverList)
    }
}

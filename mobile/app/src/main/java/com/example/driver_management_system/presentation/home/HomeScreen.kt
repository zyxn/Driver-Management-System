package com.example.driver_management_system.presentation.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.presentation.drivers.DriverListScreen
import com.example.driver_management_system.presentation.notification.NotificationScreen
import com.example.driver_management_system.presentation.profile.ProfileScreen
import com.example.driver_management_system.presentation.settings.SettingsScreen
import com.example.driver_management_system.presentation.settings.SettingsViewModel
import com.example.driver_management_system.ui.components.AppDrawer
import com.example.driver_management_system.ui.components.AppLayout
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showNotificationScreen by remember { mutableStateOf(false) }
    var showDriverListScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    
    // Initialize ViewModel
    val preferencesManager = remember { PreferencesManager(context) }
    val settingsViewModel = remember { SettingsViewModel(preferencesManager) }
    
    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            settingsViewModel.setSelectedRingtone(it.toString())
        }
    }
    
    // Handle back press based on current state
    BackHandler(enabled = drawerState.isOpen || selectedTab != 0) {
        when {
            // If drawer is open, close it
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            // If not on Dasbor tab (tab 0), go back to Dasbor
            selectedTab != 0 -> {
                selectedTab = 0
            }
        }
    }
    
    // Show profile screen
    if (showProfileScreen) {
        ProfileScreen(
            onBack = { showProfileScreen = false }
        )
        return
    }
    
    // Show settings screen
    if (showSettingsScreen) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onBack = { showSettingsScreen = false },
            onPickCustomAudio = {
                audioPickerLauncher.launch("audio/*")
            }
        )
        return
    }
    
    // Show driver list screen
    if (showDriverListScreen) {
        DriverListScreen(
            onBack = { showDriverListScreen = false }
        )
        return
    }
    
    // Show notification screen
    if (showNotificationScreen) {
        NotificationScreen(
            onBack = { showNotificationScreen = false }
        )
        return
    }

    // Main layout with drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                AppDrawer(
                    username = username,
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToProfile = {
                        showProfileScreen = true
                    },
                    onNavigateToNotifications = {
                        showNotificationScreen = true
                    },
                    onNavigateToSettings = {
                        showSettingsScreen = true
                    }
                )
            }
        }
    ) {
        AppLayout(
            title = when(selectedTab) {
                0 -> "Dasbor"
                1 -> "Perjalanan"
                2 -> "Aksi"
                else -> "Home"
            },
            username = username,
            selectedTab = selectedTab,
            notificationCount = 3,
            onAvatarClick = {
                scope.launch { drawerState.open() }
            },
            onNotificationClick = {
                showNotificationScreen = true
            },
            onTabSelected = { tab ->
                selectedTab = tab
            }
        ) { paddingValues ->
            when(selectedTab) {
                0 -> DasborScreen(
                    username = username,
                    onNavigateToDriverList = { showDriverListScreen = true }
                )
                1 -> PerjalananScreen()
                2 -> AksiScreen()
            }
        }
    }
}

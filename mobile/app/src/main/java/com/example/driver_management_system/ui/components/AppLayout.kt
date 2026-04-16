package com.example.driver_management_system.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLayout(
    title: String,
    username: String,
    selectedTab: Int,
    notificationCount: Int = 0,
    onAvatarClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onAvatarClick) {
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = username.take(2).uppercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        BadgedBox(
                            badge = {
                                if (notificationCount > 0) {
                                    Badge(
                                        containerColor = Error,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = notificationCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Outlined.Dashboard, 
                            contentDescription = "Dasbor"
                        ) 
                    },
                    label = { Text("Dasbor") },
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = Primary.copy(alpha = 0.1f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Outlined.DirectionsCar, 
                            contentDescription = "Perjalanan"
                        ) 
                    },
                    label = { Text("Perjalanan") },
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = Primary.copy(alpha = 0.1f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Outlined.TouchApp, 
                            contentDescription = "Aksi"
                        ) 
                    },
                    label = { Text("Aksi") },
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = Primary.copy(alpha = 0.1f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBase)
                .padding(paddingValues)
        ) {
            content(paddingValues)
        }
    }
}

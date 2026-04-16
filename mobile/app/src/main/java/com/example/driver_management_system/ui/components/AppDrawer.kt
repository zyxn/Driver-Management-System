package com.example.driver_management_system.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.ui.theme.*

@Composable
fun AppDrawer(
    username: String,
    email: String = "driver@example.com",
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        // Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onCloseDrawer) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Drawer",
                    tint = TextSecondary
                )
            }
        }
        
        // User Profile
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = username.take(2).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = username,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Text(
                text = email,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = BorderBase
        )
        
        // Menu Items
        DrawerMenuItem(
            icon = Icons.Outlined.Person,
            title = "Profile",
            onClick = {
                onCloseDrawer()
                onNavigateToProfile()
            }
        )
        
        DrawerMenuItem(
            icon = Icons.Outlined.Notifications,
            title = "Notifications",
            onClick = {
                onCloseDrawer()
                onNavigateToNotifications()
            }
        )
        
        DrawerMenuItem(
            icon = Icons.Outlined.Settings,
            title = "Settings",
            onClick = {
                onCloseDrawer()
                onNavigateToSettings()
            }
        )
        
        DrawerMenuItem(
            icon = Icons.Outlined.Help,
            title = "Help & Support",
            onClick = onCloseDrawer
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = BorderBase
        )
        
        // Logout
        DrawerMenuItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Logout",
            onClick = onLogout,
            textColor = Error
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = textColor
        )
    }
}

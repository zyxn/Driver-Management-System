package com.example.driver_management_system.presentation.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
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
fun NotificationListItem(
    title: String,
    message: String,
    time: String,
    isUnread: Boolean,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Handle notification click */ },
        color = if (isUnread) Primary.copy(alpha = 0.05f) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = if (isUnread) Primary.copy(alpha = 0.15f) else BorderLight,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isUnread) Primary else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Surface(
                            shape = CircleShape,
                            color = Primary,
                            modifier = Modifier
                                .size(10.dp)
                                .padding(top = 4.dp)
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = BorderLight, thickness = 1.dp)
}

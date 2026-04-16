package com.example.driver_management_system.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.driver_management_system.ui.components.AntMenuCard
import com.example.driver_management_system.ui.theme.*

@Composable
fun MenuGrid(onNavigateToDriverList: () -> Unit = {}) {
    val menuItems = listOf(
        MenuItem(
            title = "Daftar Driver",
            icon = Icons.Outlined.People,
            iconColor = Blue,
            iconBackground = Blue.copy(alpha = 0.1f),
            onClick = onNavigateToDriverList
        ),
        MenuItem(
            title = "Laporan",
            icon = Icons.Outlined.Assessment,
            iconColor = Purple,
            iconBackground = Purple.copy(alpha = 0.1f),
            onClick = {}
        ),
        MenuItem(
            title = "Kendaraan",
            icon = Icons.Outlined.DirectionsCar,
            iconColor = Cyan,
            iconBackground = Cyan.copy(alpha = 0.1f),
            onClick = {}
        )
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        menuItems.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    AntMenuCard(
                        title = item.title,
                        icon = item.icon,
                        iconColor = item.iconColor,
                        iconBackground = item.iconBackground,
                        onClick = item.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty spaces if row is not complete
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackground: Color,
    val onClick: () -> Unit = {}
)

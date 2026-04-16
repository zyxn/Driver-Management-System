package com.example.driver_management_system.presentation.notification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    // Handle back gesture
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifikasi",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Mark all as read */ }) {
                        Text(
                            "Tandai Dibaca",
                            color = Primary,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBase)
                .padding(paddingValues)
        ) {
            NotificationListItem(
                title = "Driver Baru Terdaftar",
                message = "Ahmad Rizki telah terdaftar sebagai driver baru di sistem",
                time = "5 menit yang lalu",
                isUnread = true,
                icon = Icons.Outlined.PersonAdd
            )
            
            NotificationListItem(
                title = "Perjalanan Selesai",
                message = "Budi Santoso menyelesaikan perjalanan ke Jakarta dengan sukses",
                time = "1 jam yang lalu",
                isUnread = true,
                icon = Icons.Outlined.CheckCircle
            )
            
            NotificationListItem(
                title = "Jadwal Besok",
                message = "5 driver dijadwalkan untuk perjalanan besok. Pastikan semua siap.",
                time = "3 jam yang lalu",
                isUnread = false,
                icon = Icons.Outlined.CalendarToday
            )
            
            NotificationListItem(
                title = "Pemeliharaan Kendaraan",
                message = "Kendaraan B 1234 XYZ perlu servis rutin dalam 3 hari",
                time = "5 jam yang lalu",
                isUnread = false,
                icon = Icons.Outlined.Build
            )
            
            NotificationListItem(
                title = "Laporan Harian",
                message = "Laporan harian telah tersedia untuk dilihat dan diunduh",
                time = "1 hari yang lalu",
                isUnread = false,
                icon = Icons.Outlined.Assessment
            )
            
            NotificationListItem(
                title = "Pembayaran Diterima",
                message = "Pembayaran untuk perjalanan #12345 telah diterima",
                time = "2 hari yang lalu",
                isUnread = false,
                icon = Icons.Outlined.Payment
            )
        }
    }
}

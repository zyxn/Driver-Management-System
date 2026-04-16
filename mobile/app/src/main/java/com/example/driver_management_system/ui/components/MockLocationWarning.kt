package com.example.driver_management_system.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MockLocationWarningBanner(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3CD) // Warning yellow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Mock Location Terdeteksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF856404)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Matikan Mock Location di Developer Options untuk menggunakan fitur tracking lokasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF856404)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        // Open developer options
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to general settings
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Developer Options")
                }
            }
        }
    }
}

@Composable
fun MockLocationDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Mock Location Terdeteksi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Aplikasi mendeteksi bahwa Mock Location aktif di perangkat Anda.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Untuk menggunakan fitur tracking lokasi, silakan:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Buka Developer Options")
                Text("2. Cari 'Select mock location app' atau 'Allow mock locations'")
                Text("3. Matikan atau pilih 'None'")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fitur ini diperlukan untuk memastikan lokasi yang dilaporkan adalah lokasi asli.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Buka Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

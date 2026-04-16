package com.example.driver_management_system.presentation.settings

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onPickRingtone: () -> Unit = {},
    onPickCustomAudio: () -> Unit = {}
) {
    // Handle back gesture
    BackHandler(onBack = onBack)
    
    val context = LocalContext.current
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val selectedRingtone by viewModel.selectedRingtone.collectAsState()
    
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    
    // Get ringtone display name
    val ringtoneDisplayName = remember(selectedRingtone) {
        if (selectedRingtone.startsWith("content://") || selectedRingtone.startsWith("file://")) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(selectedRingtone))
                ringtone.getTitle(context)
            } catch (e: Exception) {
                "Custom Audio"
            }
        } else {
            selectedRingtone
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Pengaturan",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali"
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
                .verticalScroll(rememberScrollState())
        ) {
            // Notifications Section
            SettingsSection(title = "Notifikasi") {
                SettingsMenuItem(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Nada Dering",
                    value = ringtoneDisplayName,
                    onClick = { showRingtoneDialog = true }
                )
                
                HorizontalDivider(color = BorderBase)
                
                SettingsSwitchItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Getar",
                    description = "Getarkan perangkat untuk notifikasi",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Settings Section
            SettingsSection(title = "Aplikasi") {
                SettingsMenuItem(
                    icon = Icons.Outlined.Language,
                    title = "Bahasa",
                    value = "Indonesia",
                    onClick = { /* TODO: Navigate to language selection */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Privacy & Security Section
            SettingsSection(title = "Akun") {
                SettingsMenuItem(
                    icon = Icons.Outlined.Lock,
                    title = "Ubah Password",
                    onClick = { showChangePasswordDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // About Section
            SettingsSection(title = "Tentang") {
                SettingsMenuItem(
                    icon = Icons.Outlined.Info,
                    title = "Tentang Aplikasi",
                    value = "Versi 1.0.0",
                    onClick = { showAboutDialog = true }
                )
                
                HorizontalDivider(color = BorderBase)
                
                SettingsMenuItem(
                    icon = Icons.Outlined.Description,
                    title = "Syarat & Ketentuan",
                    onClick = { showTermsDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Change Password Dialog
        if (showChangePasswordDialog) {
            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                containerColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                title = {
                    Text(
                        text = "Ubah Password",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "Untuk mengubah password, silakan hubungi operator atau administrator sistem.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showChangePasswordDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        
        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                title = {
                    Text(
                        text = "Tentang Aplikasi",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Driver Management System",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Versi 1.0.0",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aplikasi manajemen driver untuk memantau dan mengelola aktivitas pengemudi secara real-time.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showAboutDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Terms Dialog
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                containerColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                title = {
                    Text(
                        text = "Syarat & Ketentuan",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "1. Penggunaan Aplikasi",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aplikasi ini hanya untuk penggunaan resmi oleh driver yang terdaftar dalam sistem manajemen perusahaan.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "2. Pelacakan Lokasi",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dengan menggunakan aplikasi ini, Anda menyetujui pelacakan lokasi real-time selama jam kerja untuk keperluan operasional.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "3. Keamanan Data",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Anda bertanggung jawab untuk menjaga kerahasiaan kredensial login Anda. Jangan membagikan informasi akun kepada pihak lain.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "4. Pelaporan",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Driver wajib melaporkan setiap insiden atau masalah yang terjadi selama perjalanan melalui fitur pelaporan yang tersedia.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showTermsDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Ringtone Selection Dialog
        if (showRingtoneDialog) {
            var isLoading by remember { mutableStateOf(true) }
            var currentPlayingRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }
            
            // Get system ringtones
            val ringtoneManager = remember { RingtoneManager(context) }
            val systemRingtones = remember {
                val ringtones = mutableListOf<Pair<String, Uri>>()
                try {
                    ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
                    val cursor = ringtoneManager.cursor
                    
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = ringtoneManager.getRingtoneUri(cursor.position)
                        ringtones.add(Pair(title, uri))
                    }
                    cursor.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
                ringtones
            }
            
            // Stop ringtone when dialog is dismissed
            DisposableEffect(Unit) {
                onDispose {
                    currentPlayingRingtone?.stop()
                }
            }
            
            AlertDialog(
                onDismissRequest = { 
                    currentPlayingRingtone?.stop()
                    showRingtoneDialog = false 
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                title = {
                    Text(
                        text = "Pilih Nada Dering",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                containerColor = Color.White,
                text = {
                    if (isLoading) {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Memuat nada dering...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .background(Color.White)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Custom audio option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPlayingRingtone?.stop()
                                        showRingtoneDialog = false
                                        onPickCustomAudio()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Pilih dari File Manager",
                                    fontSize = 16.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = BorderBase
                            )
                            
                            // System ringtones
                            Text(
                                text = "Nada Dering Sistem",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            )
                            
                            if (systemRingtones.isEmpty()) {
                                Text(
                                    text = "Tidak ada nada dering sistem",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                systemRingtones.forEach { (title, uri) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // Stop previous ringtone
                                                currentPlayingRingtone?.stop()
                                                
                                                // Save selection
                                                viewModel.setSelectedRingtone(uri.toString())
                                                
                                                // Play preview
                                                try {
                                                    val ringtone = RingtoneManager.getRingtone(context, uri)
                                                    ringtone.play()
                                                    currentPlayingRingtone = ringtone
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedRingtone == uri.toString(),
                                            onClick = {
                                                // Stop previous ringtone
                                                currentPlayingRingtone?.stop()
                                                
                                                // Save selection
                                                viewModel.setSelectedRingtone(uri.toString())
                                                
                                                // Play preview
                                                try {
                                                    val ringtone = RingtoneManager.getRingtone(context, uri)
                                                    ringtone.play()
                                                    currentPlayingRingtone = ringtone
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Primary,
                                                unselectedColor = TextSecondary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            fontSize = 15.sp,
                                            color = if (selectedRingtone == uri.toString()) TextPrimary else TextSecondary,
                                            fontWeight = if (selectedRingtone == uri.toString()) FontWeight.Medium else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            currentPlayingRingtone?.stop()
                            showRingtoneDialog = false 
                        }
                    ) {
                        Text("Selesai")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

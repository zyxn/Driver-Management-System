package com.example.driver_management_system.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.driver_management_system.MainActivity
import com.example.driver_management_system.R
import com.example.driver_management_system.data.local.PreferencesManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // TODO: Send token to backend
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Extract data even if it's a "notification" type message
        // Note: For background notifications to trigger this code, 
        // the sender MUST use the 'data' payload only.
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Notification"
        val body = data["body"] ?: message.notification?.body ?: "You have a new message"
        val type = data["type"]

        when (type) {
            "overspeed" -> {
                val speed = data["speed"]?.toDoubleOrNull() ?: 0.0
                val limit = data["limit"]?.toDoubleOrNull() ?: 0.0
                handleOverspeedNotification(title, body, speed, limit)
            }
            "overspeed_operator" -> {
                handleOperatorNotification(title, body)
            }
            "alert" -> {
                handleAlertNotification(title, body)
            }
            else -> {
                handleDefaultNotification(title, body)
            }
        }
    }

    private fun handleOverspeedNotification(title: String, body: String, speed: Double, limit: Double) {
        sendNotification(
            title = title,
            body = body,
            channelId = CHANNEL_ID_ALERT,
            channelName = "Speed Alerts",
            priority = NotificationCompat.PRIORITY_HIGH,
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    private fun handleOperatorNotification(title: String, body: String) {
        sendNotification(
            title = title,
            body = body,
            channelId = CHANNEL_ID_OPERATOR,
            channelName = "Operator Alerts",
            priority = NotificationCompat.PRIORITY_DEFAULT,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun handleAlertNotification(title: String, body: String) {
        sendNotification(
            title = title,
            body = body,
            channelId = CHANNEL_ID_ALERT,
            channelName = "Management Alerts",
            priority = NotificationCompat.PRIORITY_HIGH,
            importance = NotificationManager.IMPORTANCE_HIGH,
            useRichStyle = true
        )
    }

    private fun handleDefaultNotification(title: String, body: String) {
        sendNotification(
            title = title,
            body = body,
            channelId = CHANNEL_ID_DEFAULT,
            channelName = "General Notifications",
            priority = NotificationCompat.PRIORITY_DEFAULT,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun sendNotification(
        title: String,
        body: String,
        channelId: String,
        channelName: String,
        priority: Int,
        importance: Int,
        useRichStyle: Boolean = false
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get user preferences using synchronous methods
        val preferencesManager = PreferencesManager(this)
        val vibrationEnabled = preferencesManager.getVibrationEnabled()
        val selectedRingtone = preferencesManager.getSelectedRingtone()
        
        // Create a dynamic channel ID based on preferences
        // Since NotificationChannels are immutable once created, we must use a unique ID 
        // when settings change to force the system to apply new sound/vibration.
        val prefsSuffix = "${selectedRingtone.hashCode()}_${vibrationEnabled}"
        val dynamicChannelId = "${channelId}_$prefsSuffix"
        
        // Use custom ringtone or default
        val soundUri = if (selectedRingtone.isNotEmpty()) {
            Uri.parse(selectedRingtone)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, dynamicChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setColor(getColor(R.color.purple_500))
        
        // Apply vibration based on user preference
        if (vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        } else {
            notificationBuilder.setVibrate(null) // No vibration
        }
        
        // Add rich style for longer messages
        if (useRichStyle) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
            )
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old versions of this base channel if they exist (optional, but keeps system clean)
            // Note: In a real app, you might want to keep track of previous dynamic IDs to delete them.
            
            val channel = NotificationChannel(
                dynamicChannelId,
                channelName,
                importance
            ).apply {
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                
                // Set sound
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
                
                // Set vibration based on user preference
                if (vibrationEnabled) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                } else {
                    enableVibration(false)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendTokenToServer(token: String) {
        // Store token locally first
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        
        // TODO: Send to backend API
        // This will be called from the app after login
    }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID_ALERT = "speed_alerts"
        private const val CHANNEL_ID_OPERATOR = "operator_alerts"
        private const val CHANNEL_ID_DEFAULT = "default_notifications"
    }
}

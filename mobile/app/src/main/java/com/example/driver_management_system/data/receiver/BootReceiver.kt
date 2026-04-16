package com.example.driver_management_system.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.driver_management_system.data.service.LocationTrackingService

/**
 * Automatically restarts the LocationTrackingService after device boot
 * if tracking was active when the phone was turned off.
 * 
 * This ensures drivers don't lose tracking just because their phone restarted.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            // Check if we were tracking before reboot
            val prefs = context.getSharedPreferences("location_tracking_prefs", Context.MODE_PRIVATE)
            val wasTracking = prefs.getBoolean("is_tracking", false)
            
            if (wasTracking) {
                val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                    setPackage(context.packageName)
                    // No action = triggers resume logic in onStartCommand
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}

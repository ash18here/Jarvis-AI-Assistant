package com.jarvis.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Without this, Jarvis would only start after the user manually opens
 * the app and taps "Start Jarvis" again every time the phone reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, JarvisService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}

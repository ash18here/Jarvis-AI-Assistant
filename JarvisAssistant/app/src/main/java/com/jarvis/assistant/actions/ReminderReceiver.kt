package com.jarvis.assistant.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Fires when a reminder set via the `set_reminder` tool comes due.
 * Shows a heads-up notification with the reminder text.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK = "extra_task"
        private const val CHANNEL_ID = "jarvis_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getStringExtra(EXTRA_TASK) ?: "Reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Jarvis Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Jarvis reminder")
            .setContentText(task)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }
}

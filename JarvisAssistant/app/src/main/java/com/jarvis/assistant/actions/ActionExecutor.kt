package com.jarvis.assistant.actions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import com.jarvis.assistant.brain.JarvisDecision
import org.json.JSONObject
import java.io.File

/**
 * Where the actual work happens. GroqClient decides WHAT to do,
 * ActionExecutor decides HOW to do it on the phone.
 *
 * To add a new capability:
 *   1. Define the tool in ToolRegistry.kt
 *   2. Add a matching "toolName" branch below
 */
class ActionExecutor(private val context: Context) {

    fun execute(decision: JarvisDecision.RunTool): String {
        val args = decision.arguments
        return try {
            when (decision.toolName) {
                "make_call" -> makeCall(args)
                "open_app" -> openApp(args)
                "write_note" -> writeNote(args)
                "send_message" -> sendMessage(args)
                "set_reminder" -> setReminder(args)
                "set_alarm" -> setAlarm(args)
                "toggle_flashlight" -> toggleFlashlight(args)
                "check_battery" -> checkBattery()
                "search_web" -> searchWeb(args)
                else -> "I don't know how to do '${decision.toolName}' yet."
            }
        } catch (e: SecurityException) {
            "I need permission to do that — check app permissions in Settings."
        } catch (e: Exception) {
            "Something went wrong: ${e.message}"
        }
    }

    // --- Individual actions -------------------------------------------------

    private fun makeCall(args: JSONObject): String {
        val contactName = args.getString("contact_name")
        val number = lookupPhoneNumber(contactName)
            ?: return "I couldn't find a contact named $contactName."

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Calling $contactName."
    }

    private fun openApp(args: JSONObject): String {
        val appName = args.getString("app_name")
        val pm = context.packageManager

        // Simple name -> package lookup. Extend this map as needed, or
        // search installed packages by matching their display label.
        val knownPackages = mapOf(
            "notepad" to "com.google.android.keep",
            "whatsapp" to "com.whatsapp",
            "gmail" to "com.google.android.gm"
        )

        val packageName = knownPackages[appName.lowercase()]
            ?: return "I don't know which app '$appName' refers to yet — add it to knownPackages in ActionExecutor.kt."

        val intent = pm.getLaunchIntentForPackage(packageName)
            ?: return "$appName doesn't seem to be installed."

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Opening $appName."
    }

    private fun writeNote(args: JSONObject): String {
        val content = args.getString("content")
        val notesDir = File(context.filesDir, "jarvis_notes")
        if (!notesDir.exists()) notesDir.mkdirs()

        val file = File(notesDir, "notes.txt")
        file.appendText("${System.currentTimeMillis()}: $content\n")
        return "Noted it down."
    }

    private fun sendMessage(args: JSONObject): String {
        val contactName = args.getString("contact_name")
        val message = args.getString("message")
        val number = lookupPhoneNumber(contactName)
            ?: return "I couldn't find a contact named $contactName."

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Message ready to send to $contactName."
    }

    private fun setReminder(args: JSONObject): String {
        val task = args.getString("task")
        val minutesFromNow = args.getString("minutes_from_now").toDoubleOrNull()?.toLong()
            ?: return "I couldn't understand how many minutes from now."

        val triggerAtMillis = System.currentTimeMillis() + minutesFromNow * 60_000L

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK, task)
        }
        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )

        return "Reminder set for $minutesFromNow minutes from now: $task."
    }

    private fun setAlarm(args: JSONObject): String {
        val hour = args.getString("hour").toIntOrNull()
            ?: return "I couldn't understand the hour."
        val minute = args.optString("minute", "0").toIntOrNull() ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Setting an alarm for $hour:${minute.toString().padStart(2, '0')}."
    }

    private fun toggleFlashlight(args: JSONObject): String {
        val state = args.getString("state").lowercase()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull()
            ?: return "This device doesn't seem to have a flashlight."

        cameraManager.setTorchMode(cameraId, state == "on")
        return "Flashlight turned $state."
    }

    private fun checkBattery(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        return "Battery is at $level percent" + if (isCharging) ", and charging." else "."
    }

    private fun searchWeb(args: JSONObject): String {
        val query = args.getString("query")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Searching the web for $query."
    }

    // --- Helpers -------------------------------------------------------------

    private fun lookupPhoneNumber(name: String): String? {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numberIndex)
            }
        }
        return null
    }
}

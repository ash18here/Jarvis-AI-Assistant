package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.actions.ActionExecutor
import com.jarvis.assistant.brain.GroqClient
import com.jarvis.assistant.brain.JarvisDecision
import com.jarvis.assistant.voice.JarvisSpeaker
import com.jarvis.assistant.voice.VoskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The always-on foreground service: this IS Jarvis.
 *
 * Uses Vosk for continuous offline wake-word + command listening - a
 * single recognizer runs the whole time, muted/unmuted as needed,
 * rather than being recreated per utterance.
 */
class JarvisService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var voskEngine: VoskEngine
    private lateinit var speaker: JarvisSpeaker
    private lateinit var groqClient: GroqClient
    private lateinit var actionExecutor: ActionExecutor
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile private var listeningForCommand = false

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        acquireWakeLock()

        // TODO: move this key into a secure config (BuildConfig / EncryptedSharedPreferences)
        // instead of hardcoding - never commit real keys to source control.
        val groqApiKey = BuildConfigPlaceholder.GROQ_API_KEY

        groqClient = GroqClient(groqApiKey)
        actionExecutor = ActionExecutor(applicationContext)
        speaker = JarvisSpeaker(applicationContext)
        voskEngine = VoskEngine(applicationContext)

        voskEngine.init(
            onReady = {
                mainHandler.post {
                    voskEngine.startListening(::handleRecognizedText)
                }
            },
            onError = { message ->
                android.util.Log.e("JarvisService", "Vosk failed to load: $message")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "JarvisAssistant::WakeWordLock"
        )
        wakeLock?.acquire()
    }

    private fun handleRecognizedText(text: String) {
        android.util.Log.d("JarvisWake", "Heard: '$text'")
        if (listeningForCommand) {
            listeningForCommand = false
            handleCommand(text)
        } else if (text.contains("jarvis", ignoreCase = true)) {
            onWakeWordHeard()
        }
    }

    private fun onWakeWordHeard() {
        voskEngine.setPaused(true)
        speaker.speak("Yes?") {
            mainHandler.post {
                listeningForCommand = true
                voskEngine.setPaused(false)
            }
        }
    }

    private fun handleCommand(spokenText: String) {
        voskEngine.setPaused(true)
        scope.launch {
            val decision = groqClient.interpret(spokenText)
            val resultMessage = when (decision) {
                is JarvisDecision.RunTool -> actionExecutor.execute(decision)
                is JarvisDecision.SpeakOnly -> decision.message
            }
            speaker.speak(resultMessage) {
                mainHandler.post {
                    voskEngine.setPaused(false)
                }
            }
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "jarvis_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Jarvis", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis is listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        voskEngine.destroy()
        speaker.shutdown()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object BuildConfigPlaceholder {
    const val GROQ_API_KEY = "YOUR_GROQ_API_KEY"
}

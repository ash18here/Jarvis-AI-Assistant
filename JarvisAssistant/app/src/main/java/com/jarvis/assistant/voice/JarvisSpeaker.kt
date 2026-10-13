package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Lets Jarvis speak results back to the user instead of only showing
 * text. Wraps Android's built-in TextToSpeech engine — free, offline,
 * no extra API needed.
 */
class JarvisSpeaker(context: Context) {

    private var isReady = false
    private var pendingText: String? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts.language = Locale.getDefault()
                pendingText?.let { speak(it) }
                pendingText = null
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            pendingText = text
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        if (onDone != null) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = onDone()
                override fun onError(utteranceId: String?) {}
            })
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

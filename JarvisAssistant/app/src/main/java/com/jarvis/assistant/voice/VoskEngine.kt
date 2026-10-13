package com.jarvis.assistant.voice

import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import android.content.Context
import java.io.IOException

/**
 * Offline, on-device speech recognition using Vosk. Unlike Android's
 * built-in SpeechRecognizer, this doesn't depend on Google's cloud
 * recognition service - it runs entirely on-device and keeps listening
 * continuously without being torn down and recreated between utterances.
 */
class VoskEngine(private val context: Context) {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var resultCallback: ((String) -> Unit)? = null

    /** Unpacks the model from assets/model-en-us on first run, then loads it. */
    fun init(onReady: () -> Unit, onError: (String) -> Unit) {
        StorageService.unpack(
            context, "model-en-us", "model",
            { loadedModel: Model ->
                model = loadedModel
                onReady()
            },
            { exception: IOException ->
                onError(exception.message ?: "Failed to load speech model")
            }
        )
    }

    /** Starts continuous listening. onResult fires each time a phrase completes. */
    fun startListening(onResult: (String) -> Unit) {
        resultCallback = onResult
        val loadedModel = model ?: return

        val recognizer = Recognizer(loadedModel, 16000.0f)
        val service = SpeechService(recognizer, 16000.0f)
        speechService = service

        service.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {}

            override fun onResult(hypothesis: String?) {
                val text = extractText(hypothesis)
                if (text.isNotBlank()) resultCallback?.invoke(text)
            }

            override fun onFinalResult(hypothesis: String?) {
                val text = extractText(hypothesis)
                if (text.isNotBlank()) resultCallback?.invoke(text)
            }

            override fun onError(exception: Exception?) {}

            override fun onTimeout() {}
        })
    }

    /** Mutes/unmutes the mic without tearing down the recognizer - used
     * while Jarvis is speaking, so it doesn't hear its own voice. */
    fun setPaused(paused: Boolean) {
        speechService?.setPause(paused)
    }

    fun destroy() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    private fun extractText(hypothesis: String?): String {
        if (hypothesis == null) return ""
        return try {
            JSONObject(hypothesis).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}

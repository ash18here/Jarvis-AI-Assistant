package com.jarvis.assistant

import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.app.Activity
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var statusText: TextView
    private lateinit var responseText: TextView
    private lateinit var commandInput: EditText
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        responseText = findViewById(R.id.responseText)
        commandInput = findViewById(R.id.commandInput)
        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.runButton).setOnClickListener {
            val command = commandInput.text.toString().trim()
            if (command.isBlank()) {
                respond("Please type a command first.")
            } else {
                handleCommand(command)
            }
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener { showBattery() }
        findViewById<Button>(R.id.webButton).setOnClickListener { openWebsite("https://www.google.com") }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.UK
        statusText.text = if (ttsReady) "Ready. Jarvis is online." else "Ready. Speech output unavailable."
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        when {
            command.contains("battery") -> showBattery()
            command.contains("open youtube") || command == "youtube" -> openWebsite("https://www.youtube.com")
            command.contains("open google") || command == "google" -> openWebsite("https://www.google.com")
            command == "help" || command.contains("what can you do") -> respond("I can check battery level and open Google or YouTube. Voice recognition and AI answers are planned next.")
            else -> respond("I don't know that command yet. Type help to see what works in this starter version.")
        }
    }

    private fun showBattery() {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        respond(if (level in 0..100) "Your battery is at $level percent." else "I couldn't read the battery level.")
    }

    private fun openWebsite(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            respond("Opening ${Uri.parse(url).host}.")
        } catch (_: Exception) {
            respond("I couldn't open that website on this device.")
        }
    }

    private fun respond(message: String) {
        responseText.text = message
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis-response")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

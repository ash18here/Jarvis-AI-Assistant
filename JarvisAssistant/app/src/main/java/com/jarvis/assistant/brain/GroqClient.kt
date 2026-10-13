package com.jarvis.assistant.brain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GroqClient is the "brain" of Jarvis.
 *
 * It sends the user's spoken text to Groq's LLM along with a list of
 * TOOLS (things Jarvis knows how to do). The model decides which tool
 * to call and with what parameters, and returns that decision as
 * structured JSON (function_call). We never ask the LLM to write
 * unstructured text back for actions — this keeps behavior predictable.
 *
 * TO ADD A NEW CAPABILITY TO JARVIS:
 *   1. Add a new tool definition in ToolRegistry.kt
 *   2. Add a matching `when` branch in ActionExecutor.kt
 *   That's it — the LLM will automatically start using it when relevant.
 */
class GroqClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    /**
     * Sends the user's utterance to the model along with the current
     * tool registry. Returns a JarvisDecision describing which action
     * to run (or a plain spoken reply if no tool applies).
     */
    suspend fun interpret(userText: String): JarvisDecision = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put(
                        "content",
                        "You are Jarvis, a voice assistant living inside an Android app. " +
                            "Decide which single tool best satisfies the user's request. " +
                            "If nothing fits, reply normally without calling a tool."
                    )
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            })
            put("tools", ToolRegistry.asGroqToolsJson())
            put("tool_choice", "auto")
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext JarvisDecision.SpeakOnly(
                    "Jarvis ran into a problem talking to the brain: ${response.code}"
                )
            }
            parseDecision(raw)
        }
    }

    private fun parseDecision(raw: String): JarvisDecision {
        val choice = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")

        val toolCalls = choice.optJSONArray("tool_calls")
        if (toolCalls != null && toolCalls.length() > 0) {
            val call = toolCalls.getJSONObject(0).getJSONObject("function")
            val name = call.getString("name")
            val args = JSONObject(call.getString("arguments"))
            return JarvisDecision.RunTool(name, args)
        }

        val plainText = choice.optString("content", "I didn't quite catch a task in that.")
        return JarvisDecision.SpeakOnly(plainText)
    }
}

sealed class JarvisDecision {
    data class RunTool(val toolName: String, val arguments: JSONObject) : JarvisDecision()
    data class SpeakOnly(val message: String) : JarvisDecision()
}

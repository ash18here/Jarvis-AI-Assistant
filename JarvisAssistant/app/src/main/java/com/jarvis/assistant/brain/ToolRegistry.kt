package com.jarvis.assistant.brain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Central place where every Jarvis capability is declared.
 *
 * This is THE file you edit to make Jarvis smarter. Each tool you add
 * here automatically becomes something the LLM can choose to call —
 * you don't need to touch any NLP/parsing logic, Groq handles the
 * "understanding" part for you.
 *
 * After adding a tool here, go add its real implementation as a new
 * `when` branch in ActionExecutor.kt.
 */
object ToolRegistry {

    fun asGroqToolsJson(): JSONArray {
        val tools = JSONArray()

        tools.put(tool(
            name = "make_call",
            description = "Call a contact by name using the phone dialer",
            params = mapOf("contact_name" to "The name of the contact to call, e.g. 'Rohan'")
        ))

        tools.put(tool(
            name = "open_app",
            description = "Open an app installed on the phone by its common name",
            params = mapOf("app_name" to "The name of the app to open, e.g. 'Notepad', 'WhatsApp'")
        ))

        tools.put(tool(
            name = "write_note",
            description = "Create or append a note with the given content, e.g. a daily routine or task list",
            params = mapOf("content" to "The text to write into the note")
        ))

        tools.put(tool(
            name = "send_message",
            description = "Send a text/WhatsApp message to a contact",
            params = mapOf(
                "contact_name" to "The name of the contact to message",
                "message" to "The message body to send"
            )
        ))

        tools.put(tool(
            name = "set_reminder",
            description = "Set a reminder that alerts the user after some delay",
            params = mapOf(
                "task" to "What to remind the user about",
                "minutes_from_now" to "How many minutes from now to fire the reminder, as a number string"
            )
        ))

        tools.put(tool(
            name = "set_alarm",
            description = "Open the clock app's alarm creation screen for a specific time",
            params = mapOf(
                "hour" to "Hour in 24-hour format, e.g. '7' for 7 AM or '19' for 7 PM",
                "minute" to "Minute, e.g. '30'"
            )
        ))

        tools.put(tool(
            name = "toggle_flashlight",
            description = "Turn the phone's flashlight on or off",
            params = mapOf("state" to "Either 'on' or 'off'")
        ))

        tools.put(tool(
            name = "check_battery",
            description = "Report the phone's current battery percentage and charging status",
            params = emptyMap()
        ))

        tools.put(tool(
            name = "search_web",
            description = "Search the web for a query and open the results in the browser",
            params = mapOf("query" to "What to search for")
        ))

        // Add your next capability here, following the same pattern.

        return tools
    }

    private fun tool(name: String, description: String, params: Map<String, String>): JSONObject {
        val properties = JSONObject()
        params.forEach { (key, desc) ->
            properties.put(key, JSONObject().apply {
                put("type", "string")
                put("description", desc)
            })
        }

        return JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", name)
                put("description", description)
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", properties)
                    put("required", JSONArray(params.keys.toList()))
                })
            })
        }
    }
}

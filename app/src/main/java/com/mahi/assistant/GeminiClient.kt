package com.mahi.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini API call. Response JSON format:
 * {"reply": "...", "action": null | {"type": "...", ...}}
 */
class GeminiClient {

    private val model = "gemini-3.8-flash"

    fun chat(apiKey: String, userText: String, adult: Boolean): Pair<String, JSONObject?> {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")

        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt(adult))))
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userText)))
                )
            )
            .put(
                "generationConfig",
                JSONObject().put("responseMimeType", "application/json").put("temperature", 0.7)
            )

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw RuntimeException("HTTP $code ${text.take(160)}")

        val raw = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text")
        val clean = raw.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(clean)
        val action = if (json.isNull("action")) null else json.optJSONObject("action")
        return Pair(json.optString("reply", "Ji Boss."), action)
    }

    private fun systemPrompt(adult: Boolean): String {
        val mode = if (adult) {
            "18+ mode ON: user confirmed they are an adult. You may use frank, casual adult conversation. " +
                "Never produce sexual content involving minors, non-consensual content, real people's private data, or anything illegal."
        } else {
            "18+ mode OFF: keep content family friendly."
        }
        return """
You are MAHI, a warm, friendly, female AI assistant running on the user's Android phone. Address the user as "Boss".
Reply in the same language style the user writes in (romanized Bengali, Hindi, English, Bengali script are all fine). Keep spoken replies short and natural.
You can also write code when asked; put code in the reply.

You MUST respond ONLY with valid JSON, no markdown:
{"reply": "<what MAHI says aloud>", "action": null or {"type": "...", ...}}

Allowed action types:
- open_app {"app": "<app name or package>"}
- call {"number": "<digits>"}            (only when the user clearly asked to call)
- sms {"number": "...", "text": "..."}   (only when the user clearly asked to send)
- answer_call {}
- tap_text {"text": "<visible button text on screen>"}
- type_text {"text": "..."}
- scroll_down {}  /  scroll_up {}
- back {}  /  home {}
- camera {}  /  record_start {}  /  record_stop {}

Rules: For call or sms, if the request is unclear, ask for confirmation first and set action to null.
Never claim you did something unless the matching action is set.
$mode
        """.trimIndent()
    }
}

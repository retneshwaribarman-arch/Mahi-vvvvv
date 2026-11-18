package com.mahi.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import org.json.JSONObject

/**
 * JavaScript (index.html) theke Kotlin-e call hoy: window.Android.xxx()
 * Kotlin theke JavaScript-e feedback jay: window.mahiReply(...), mahiSpeechResult(...)
 */
class MahiBridge(private val activity: MainActivity) {

    private val prefs = activity.getSharedPreferences("mahi", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val voice = VoiceEngine(activity)
    private val gemini = GeminiClient()
    private var recognizer: SpeechRecognizer? = null

    private fun js(code: String) {
        main.post { activity.web.evaluateJavascript(code, null) }
    }

    private fun apiKey(): String = prefs.getString("api_key", "") ?: ""

    private fun hasMic(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // ---------- Settings ----------

    @JavascriptInterface
    fun getSettings(): String = JSONObject()
        .put("hasKey", apiKey().isNotBlank())
        .put("adult", prefs.getBoolean("adult", false))
        .put("ageConfirmed", prefs.getBoolean("age_confirmed", false))
        .toString()

    @JavascriptInterface
    fun saveSettings(key: String, adult: Boolean, ageConfirmed: Boolean) {
        val editor = prefs.edit()
        if (key.isNotBlank()) editor.putString("api_key", key.trim())
        editor.putBoolean("age_confirmed", ageConfirmed)
        // 18+ mode tkhon-i chalu hobe jokhon user age confirm korechhe
        editor.putBoolean("adult", adult && ageConfirmed)
        editor.apply()
        js("mahiStatus('Settings save hoyeche')")
    }

    // ---------- Chat (Gemini) ----------

    @JavascriptInterface
    fun ask(text: String) {
        Thread {
            val key = apiKey()
            if (key.isBlank()) {
                deliver("Boss, age Settings e Gemini API key dao.", null)
                return@Thread
            }
            try {
                val (reply, action) = gemini.chat(key, text, prefs.getBoolean("adult", false))
                deliver(reply, action)
            } catch (e: Exception) {
                deliver("Gemini er sathe jogajog hocche na: ${e.message}", null)
            }
        }.start()
    }

    private fun deliver(reply: String, action: JSONObject?) {
        val type = action?.optString("type").orEmpty()
        var note = ""
        when (type) {
            "" -> {}
            "record_start" -> main.post { activity.launchScreenRecord() }
            "record_stop" -> activity.stopScreenRecord()
            "camera" -> main.post { activity.launchCamera() }
            else -> note = Actions.run(activity, action!!)
        }
        val spoken = if (note.isNotBlank()) "$reply $note" else reply
        voice.speak(spoken)
        js("mahiReply(${JSONObject.quote(spoken)})")
    }

    // ---------- Voice input (native SpeechRecognizer) ----------

    @JavascriptInterface
    fun startListening() {
        main.post {
            if (!hasMic()) {
                js("mahiStatus('Mic permission dao, Boss')")
                js("mahiListenEnd()")
                return@post
            }
            if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
                js("mahiStatus('Speech recognition ei phone e nei')")
                js("mahiListenEnd()")
                return@post
            }
            recognizer?.destroy()
            val sr = SpeechRecognizer.createSpeechRecognizer(activity)
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) js("mahiSpeechResult(${JSONObject.quote(text)})")
                    js("mahiListenEnd()")
                }
                override fun onError(error: Int) { js("mahiListenEnd()") }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer = sr
            sr.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
            )
        }
    }

    @JavascriptInterface
    fun stopListening() {
        main.post { recognizer?.stopListening() }
    }

    // ---------- Background service / power ----------

    @JavascriptInterface
    fun setPower(on: Boolean) {
        val intent = Intent(activity, MahiService::class.java)
        if (!on) {
            activity.stopService(intent)
            return
        }
        if (!hasMic()) {
            js("mahiStatus('Mic permission dao, Boss')")
            return
        }
        ContextCompat.startForegroundService(activity, intent)
    }

    // ---------- Phone-level helpers from the UI ----------

    @JavascriptInterface
    fun openAccessibility() {
        main.post {
            activity.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    @JavascriptInterface
    fun toggleRecording() {
        main.post {
            if (ScreenRecordService.isRecording) activity.stopScreenRecord()
            else activity.launchScreenRecord()
        }
    }

    @JavascriptInterface
    fun takePhoto() {
        main.post { activity.launchCamera() }
    }
}

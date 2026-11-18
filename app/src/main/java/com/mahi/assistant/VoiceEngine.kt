package com.mahi.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Female-leaning voice. Offline voice paile sheta prefer kore. */
class VoiceEngine(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.setLanguage(Locale.US)
        pickFemaleVoice()
        tts.setPitch(1.15f)
        tts.setSpeechRate(0.95f)
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    private fun pickFemaleVoice() {
        val voices = tts.voices ?: return
        val offline = voices.filter { !it.isNetworkConnectionRequired }
        val voice = offline.firstOrNull { it.name.contains("female", true) && it.locale.language == "en" }
            ?: offline.firstOrNull { it.name.contains("female", true) }
            ?: offline.firstOrNull { it.locale.language == "en" }
        voice?.let { tts.setVoice(it) }
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mahi-${System.nanoTime()}")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

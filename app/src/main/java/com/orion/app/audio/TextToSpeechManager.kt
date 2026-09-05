package com.orion.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Wraps Android's built-in TTS engine — real, on-device, no extra API key. */
class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("tr", "TR")
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "orion-utterance")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

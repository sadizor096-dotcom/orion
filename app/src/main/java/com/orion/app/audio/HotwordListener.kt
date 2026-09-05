package com.orion.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Hey Orion" wake phrase, built on Android's on-device SpeechRecognizer in a
 * restart loop. This is the honest, dependency-free way to do it — but be
 * aware of its real limits vs. a dedicated wake-word engine:
 *
 *  - SpeechRecognizer sessions time out after a few seconds of silence, so
 *    this loop restarts itself repeatedly. That has a real battery cost for
 *    a true "always listening" assistant.
 *  - There's a small gap between one session ending and the next starting,
 *    so the wake phrase can occasionally be missed.
 *  - It requires the phone/tablet to be unlocked and the app in foreground
 *    or running as a foreground service with a persistent notification
 *    (Android restricts background microphone access otherwise).
 *
 * Upgrade path: Picovoice Porcupine (or a similar offline wake-word SDK) is
 * purpose-built for this — much lower battery cost and near-instant
 * detection — but requires creating a free Picovoice account, generating a
 * custom "Hey Orion" .ppn keyword file in their console, and adding their
 * SDK dependency. That's a deliberate extra step on your side because it
 * needs an account only you can create; this class is the drop-in-ready
 * fallback so wake-by-voice works today without that signup.
 */
class HotwordListener(
    private val context: Context,
    private val phrase: String = "hey orion"
) {
    private var recognizer: SpeechRecognizer? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun start(onWakeDetected: () -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        stop()

        job = scope.launch {
            while (true) {
                val heard = listenOnce()
                if (heard.contains(phrase, ignoreCase = true) ||
                    heard.contains("orion", ignoreCase = true) // tolerate "Orion" alone in noisy audio
                ) {
                    onWakeDetected()
                }
                delay(150) // brief pause before the next session to avoid a tight error loop
            }
        }
    }

    fun stop() {
        job?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private suspend fun listenOnce(): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (cont.isActive) cont.resume("") { }
                }
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    if (cont.isActive) cont.resume(text) { }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            }
            startListening(intent)
        }
    }
}

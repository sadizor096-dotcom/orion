package com.orion.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

enum class ClapEvent {
    NONE,
    SINGLE_CLAP,
    DOUBLE_CLAP_DETECTED,   // wake ↔ sleep toggle
    TRIPLE_CLAP_DETECTED    // emergency shutdown
}

/**
 * Reads real microphone PCM samples via AudioRecord and looks for clap
 * patterns. Because 2 claps and 3 claps mean very different things
 * (wake/sleep vs. emergency shutdown), we can't fire on the 2nd clap
 * immediately — we wait a short grace period after the 2nd clap to see
 * if a 3rd one follows, then resolve to exactly one event.
 */
class ClapDetector(
    private val sampleRate: Int = 16000,
    private val peakThreshold: Double = 4500.0,   // tuned empirically; expose via Settings if needed
    private val minGapMs: Long = 120L,
    private val comboWindowMs: Long = 1100L,
    private val resolveGraceMs: Long = 450L        // time to wait after clap #2 for a possible #3
) {
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var resolveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _event = MutableStateFlow(ClapEvent.NONE)
    val event: StateFlow<ClapEvent> = _event

    @SuppressLint("MissingPermission") // caller guarantees permission is granted
    fun start() {
        if (job?.isActive == true) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )

        val record = audioRecord ?: return
        if (record.state != AudioRecord.STATE_INITIALIZED) return

        record.startRecording()
        _isListening.value = true

        var lastPeakAt = 0L
        var clapsInWindow = 0
        var windowStartedAt = 0L

        job = scope.launch {
            val buffer = ShortArray(minBufferSize)
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val rms = rms(buffer, read)
                    val now = System.currentTimeMillis()

                    if (rms > peakThreshold && now - lastPeakAt > minGapMs) {
                        lastPeakAt = now
                        if (now - windowStartedAt > comboWindowMs) {
                            windowStartedAt = now
                            clapsInWindow = 1
                        } else {
                            clapsInWindow++
                        }

                        when (clapsInWindow) {
                            1 -> _event.value = ClapEvent.SINGLE_CLAP
                            2 -> {
                                // Don't resolve yet — a 3rd clap within resolveGraceMs
                                // changes the meaning entirely (shutdown, not wake/sleep).
                                resolveJob?.cancel()
                                resolveJob = scope.launch {
                                    delay(resolveGraceMs)
                                    _event.value = ClapEvent.DOUBLE_CLAP_DETECTED
                                    clapsInWindow = 0
                                    windowStartedAt = 0L
                                }
                            }
                            3 -> {
                                resolveJob?.cancel()
                                _event.value = ClapEvent.TRIPLE_CLAP_DETECTED
                                clapsInWindow = 0
                                windowStartedAt = 0L
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        resolveJob?.cancel()
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) { /* already stopped */ }
        audioRecord?.release()
        audioRecord = null
        _isListening.value = false
        _event.value = ClapEvent.NONE
    }

    private fun rms(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) sum += abs(buffer[i].toDouble())
        return sqrt(sum / length) * 10 // scaled for a readable threshold constant above
    }
}

package com.orion.app.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * "Masaya 2 kere vurma" — reuses the accelerometer (real hardware sensor,
 * no extra permission needed) instead of the microphone, the same way a
 * phone's "double tap to wake" gesture works. Two sharp jolts within a
 * short window toggle Do Not Disturb.
 */
class KnockDetector(
    context: Context,
    private val jerkThreshold: Float = 18f,     // m/s^2 delta; tune per device
    private val minGapMs: Long = 100L,
    private val comboWindowMs: Long = 700L
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _doubleKnockDetected = MutableStateFlow(false)
    val doubleKnockDetected: StateFlow<Boolean> = _doubleKnockDetected

    private var lastMagnitude = 0f
    private var lastKnockAt = 0L
    private var knocksInWindow = 0
    private var windowStartedAt = 0L
    private var resetJob: Job? = null

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        resetJob?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        val jerk = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude

        val now = System.currentTimeMillis()
        if (jerk > jerkThreshold && now - lastKnockAt > minGapMs) {
            lastKnockAt = now
            if (now - windowStartedAt > comboWindowMs) {
                windowStartedAt = now
                knocksInWindow = 1
            } else {
                knocksInWindow++
            }

            if (knocksInWindow >= 2) {
                knocksInWindow = 0
                windowStartedAt = 0L
                scope.launch {
                    _doubleKnockDetected.value = true
                    delay(50) // pulse the flag so repeated collectors see a fresh event
                    _doubleKnockDetected.value = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

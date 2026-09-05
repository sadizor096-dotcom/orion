package com.orion.app.alerts

import com.orion.app.location.LatLng
import com.orion.app.network.DisasterApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DisasterAlert(val message: String, val magnitude: Double)

/**
 * Polls the (unofficial — see DisasterApiService's doc comment) earthquake
 * feed every [pollIntervalMs] and raises an alert if a quake above
 * [magnitudeThreshold] happened within [radiusKm] of the device's last
 * known location. This is a best-effort convenience feature, not a
 * certified emergency-alert system — surface that distinction to the user.
 */
class DisasterAlertMonitor(
    private val api: DisasterApiService,
    private val magnitudeThreshold: Double = 4.0,
    private val radiusKm: Double = 150.0,
    private val pollIntervalMs: Long = 120_000L
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private val seenIds = mutableSetOf<String>()

    private val _alert = MutableStateFlow<DisasterAlert?>(null)
    val alert: StateFlow<DisasterAlert?> = _alert

    fun start(getLocation: suspend () -> LatLng?) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                try {
                    val location = getLocation()
                    if (location != null) {
                        val response = api.latestEarthquakes()
                        response.result.forEach { quake ->
                            val coords = quake.geojson?.coordinates
                            if (coords != null && coords.size >= 2) {
                                val quakeLoc = LatLng(coords[1], coords[0])
                                val distance = haversineKm(location, quakeLoc)
                                val id = quake.properties.date + quake.properties.title
                                if (quake.properties.mag >= magnitudeThreshold &&
                                    distance <= radiusKm &&
                                    id !in seenIds
                                ) {
                                    seenIds.add(id)
                                    _alert.value = DisasterAlert(
                                        message = "Yakınlarda ${quake.properties.title} depremi hissedildi. " +
                                            "Yakında size de ulaşabilir.",
                                        magnitude = quake.properties.mag
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Network hiccup — stay silent and try again next cycle rather
                    // than surfacing a false alarm.
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun clearAlert() { _alert.value = null }
    fun stop() { job?.cancel() }

    private fun haversineKm(a: LatLng, b: LatLng): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).let { it * it }
        return r * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}

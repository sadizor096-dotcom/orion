package com.orion.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine

data class LatLng(val lat: Double, val lon: Double)

/**
 * Uses Android's built-in LocationManager (no Google Play Services
 * dependency needed) to get a real last-known fix. Requires
 * ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION to already be granted —
 * callers must check that first.
 */
class LocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LatLng? = suspendCancellableCoroutine { cont ->
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        var best: android.location.Location? = null
        for (provider in providers) {
            val loc = try { lm.getLastKnownLocation(provider) } catch (e: SecurityException) { null }
            if (loc != null && (best == null || loc.accuracy < best!!.accuracy)) {
                best = loc
            }
        }
        cont.resume(best?.let { LatLng(it.latitude, it.longitude) }) { }
    }
}

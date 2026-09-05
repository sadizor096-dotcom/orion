package com.orion.app.network

import retrofit2.http.GET

data class QuakeGeojsonProperties(
    val mag: Double,
    val title: String,
    val date: String
)
data class QuakeItem(
    val properties: QuakeGeojsonProperties,
    val geojson: QuakeGeojsonGeometry?
)
data class QuakeGeojsonGeometry(val coordinates: List<Double>) // [lon, lat, depth]
data class QuakeResponse(val result: List<QuakeItem>)

/**
 * IMPORTANT — read before relying on this for real safety decisions:
 * This hits a free, community-run mirror of Kandilli Rasathanesi earthquake
 * data (api.orhanaydogdu.com.tr), NOT an official AFAD/Kandilli endpoint and
 * NOT a certified early-warning system. Network delay alone means it can
 * never be as fast as Turkey's official Cell Broadcast earthquake alerts.
 * Use this only as a supplementary "ambient awareness" feature — keep
 * official channels (AFAD's own app, Cell Broadcast, local authorities) as
 * the real source of truth, and say so to the end user in your own app's UI.
 */
interface DisasterApiService {
    @GET("deprem/kandilli/live")
    suspend fun latestEarthquakes(): QuakeResponse
}

object DisasterApiModule {
    fun build(): DisasterApiService = retrofit2.Retrofit.Builder()
        .baseUrl("https://api.orhanaydogdu.com.tr/")
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
        .create(DisasterApiService::class.java)
}

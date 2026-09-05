package com.orion.app.network

import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoCurrent(
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val wind_speed_10m: Double,
    val weather_code: Int
)
data class OpenMeteoResponse(val current: OpenMeteoCurrent)

/** Free, key-less weather API — same one used in the earlier web prototype. */
interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getCurrent(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
    ): OpenMeteoResponse
}

object WeatherModule {
    fun build(): WeatherApiService = retrofit2.Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)
}

package com.orion.app.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Contract with YOUR OWN backend — never with an AI provider directly.
 * The backend is responsible for holding the provider API key and choosing
 * the model, so the model can be swapped without touching this app.
 *
 *   Android App  --https-->  ORION Backend  --->  AI Model  ---> Response
 */
data class ChatRequest(
    val message: String,
    val conversationId: String? = null
)

data class ChatResponse(
    val reply: String,
    val conversationId: String
)

data class HealthResponse(
    val status: String,
    val model: String?
)

interface OrionApiService {

    @POST("v1/chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse

    // Used both to confirm the backend is reachable AND to measure a real
    // round-trip latency number for the System HUD — never a fabricated ping.
    @GET("v1/health")
    suspend fun health(): HealthResponse
}

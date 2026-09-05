package com.orion.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ChatResult {
    data class Success(val reply: String) : ChatResult()
    data class Failure(val reason: String) : ChatResult()
}

class ChatRepository(private var api: OrionApiService) {

    fun updateBaseUrl(baseUrl: String, api: OrionApiService) {
        this.api = api
    }

    suspend fun send(message: String, conversationId: String?): ChatResult = withContext(Dispatchers.IO) {
        try {
            val response = api.sendMessage(ChatRequest(message, conversationId))
            ChatResult.Success(response.reply)
        } catch (e: Exception) {
            // Never fabricate a reply on failure — surface the real error state
            // so the UI can show it honestly instead of a fake "OK".
            ChatResult.Failure(e.message ?: "Backend unreachable")
        }
    }

    /** Real round-trip measurement used by the System HUD's NETWORK card. */
    suspend fun measureLatencyMs(): Long? = withContext(Dispatchers.IO) {
        try {
            val start = System.nanoTime()
            api.health()
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            elapsedMs
        } catch (e: Exception) {
            null
        }
    }
}

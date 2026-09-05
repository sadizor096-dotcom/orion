package com.orion.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orion.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "orion_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val MIC_ENABLED = booleanPreferencesKey("mic_enabled")
        val DOUBLE_CLAP_ENABLED = booleanPreferencesKey("double_clap_enabled")
    }

    val backendUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.BACKEND_URL] ?: BuildConfig.DEFAULT_BACKEND_URL
    }

    val micEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MIC_ENABLED] ?: true }

    val doubleClapEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.DOUBLE_CLAP_ENABLED] ?: true
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[Keys.BACKEND_URL] = url }
    }

    suspend fun setMicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MIC_ENABLED] = enabled }
    }

    suspend fun setDoubleClapEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_CLAP_ENABLED] = enabled }
    }
}

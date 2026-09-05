package com.orion.app.reminders

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

private val Context.reminderStore by preferencesDataStore(name = "orion_reminders")

data class Reminder(
    val label: String,
    val epochMillis: Long
)

/**
 * Simple local reminder store (DataStore + JSON), independent of whatever
 * clock/reminders app the user has installed. Used to answer "bugün için
 * kaç hatırlatıcınız var" in the wake briefing. See ReminderIntentHelper
 * for ALSO pushing a reminder into the user's real system reminders app.
 */
class ReminderRepository(private val context: Context) {
    private val key = stringSetPreferencesKey("reminders_json_set")
    private val gson = Gson()

    val all: Flow<List<Reminder>> = context.reminderStore.data.map { prefs ->
        (prefs[key] ?: emptySet()).mapNotNull {
            try { gson.fromJson(it, Reminder::class.java) } catch (e: Exception) { null }
        }.sortedBy { it.epochMillis }
    }

    suspend fun add(reminder: Reminder) {
        context.reminderStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + gson.toJson(reminder)
        }
    }

    suspend fun todayCount(): Int {
        val today = LocalDate.now(ZoneId.systemDefault())
        return all.first().count { reminder ->
            Instant.ofEpochMilli(reminder.epochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }
}

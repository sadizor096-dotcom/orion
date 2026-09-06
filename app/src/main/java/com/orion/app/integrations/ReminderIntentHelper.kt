package com.orion.app.integrations

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract

object ReminderIntentHelper {

    fun setReminder(context: Context, title: String, startEpochMillis: Long, durationMinutes: Int = 30) {
        val endEpochMillis = startEpochMillis + durationMinutes * 60_000L
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startEpochMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endEpochMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

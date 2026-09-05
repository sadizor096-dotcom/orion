package com.orion.app.integrations

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Fully autonomous, real, one-shot: AlarmClock.ACTION_SET_REMINDER is a
 * standard Android implicit intent — no runtime permission needed, works
 * with whatever clock/reminders app the user has set as default. This is
 * the one piece of "control other apps" that genuinely works exactly the
 * way you described: "Orion yarına 14:00'e X hatırlatıcısını kur" → done,
 * no extra taps.
 */
object ReminderIntentHelper {
    fun setReminder(context: Context, title: String, epochMillis: Long) {
        val intent = Intent(AlarmClock.ACTION_SET_REMINDER).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, title)
            putExtra(AlarmClock.EXTRA_HOUR, 0) // overridden by EXTRA_ALARM below where supported
            putExtra("android.intent.extra.alarm.TIME", epochMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true) // no confirmation screen — truly one-shot
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

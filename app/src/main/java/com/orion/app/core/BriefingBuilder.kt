package com.orion.app.core

import com.orion.app.system.BatteryStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the exact spoken sentence requested:
 * "Orion online. Saat ..., hava ... derece, pil ..., bugün için ... adlı
 *  hatırlatıcılarınız var." — every "..." is filled with the real value at
 * the moment of speaking, never a placeholder.
 */
object BriefingBuilder {
    fun build(
        battery: BatteryStats?,
        temperatureC: Double?,
        todayReminderCount: Int
    ): String {
        val time = SimpleDateFormat("HH:mm", Locale("tr")).format(Date())
        val batteryPart = battery?.takeIf { it.percent >= 0 }?.let { "%${it.percent}" } ?: "bilinmiyor"
        val weatherPart = temperatureC?.let { "${it.toInt()} derece" } ?: "şu anda alınamadı"
        val reminderPart = if (todayReminderCount > 0) {
            "bugün için $todayReminderCount adet hatırlatıcınız var"
        } else {
            "bugün için hatırlatıcınız yok"
        }

        return "Orion online. Saat $time, hava $weatherPart, pil $batteryPart, $reminderPart."
    }
}

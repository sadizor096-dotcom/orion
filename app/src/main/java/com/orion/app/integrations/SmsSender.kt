package com.orion.app.integrations

import android.content.Context
import android.telephony.SmsManager

/**
 * A CORRECTION to something I said earlier: Android does NOT require your
 * app to be the default SMS handler to send texts programmatically. Any
 * app holding the SEND_SMS runtime permission can call SmsManager directly,
 * and it sends immediately — no user tap, no confirmation screen. Being the
 * "default SMS app" is a separate, unrelated thing (it's about RECEIVING/
 * reading the system SMS inbox, and about Google Play's distribution policy
 * if you ever publish this — irrelevant for a personal/sideloaded build).
 *
 * So for SMS specifically, "1 kere izin ver, sonra otomatik gönder" is
 * exactly what this class does — genuinely, not a workaround.
 *
 * Requires in the manifest:
 *   <uses-permission android:name="android.permission.SEND_SMS" />
 * plus one runtime ActivityResultContracts.RequestPermission() grant
 * (same pattern MainActivity already uses for RECORD_AUDIO).
 */
class SmsSender(private val context: Context) {

    fun send(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

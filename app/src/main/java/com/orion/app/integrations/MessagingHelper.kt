package com.orion.app.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

data class ContactMatch(val name: String, val phoneNumber: String)

/**
 * "Orion, X'e '...' yaz" — real contact lookup + a real message pre-filled
 * in the target app. READ THE LIMIT BELOW, it's an Android security
 * boundary, not a missing feature:
 *
 * Android does NOT allow a regular third-party app to silently send an SMS
 * or a WhatsApp message on the user's behalf. Only the device's default
 * SMS app can send SMS without the user tapping Send (that's intentional —
 * it's the same protection that stops apps from silently sending premium-
 * rate spam texts from your phone). WhatsApp has no public API for this at
 * all outside their separate Business API product.
 *
 * So this class does the honest maximum: finds the contact, opens the
 * chosen app with the message already typed in, and the user does one
 * final tap to send. If you want a truly zero-tap send, the only Android
 * mechanism for that is an AccessibilityService that taps the Send button
 * for you — see OrionAccessibilityService.kt for that option and its
 * (real, meaningful) privacy trade-off before turning it on.
 */
class MessagingHelper(private val context: Context) {

    /** Requires READ_CONTACTS permission to already be granted. */
    fun findContact(name: String): ContactMatch? {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        ) ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(0)
                val number = it.getString(1)
                return ContactMatch(displayName, number)
            }
        }
        return null
    }

    /** Opens the default SMS app with the contact + message pre-filled. */
    fun prefillSms(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Opens WhatsApp with the contact + message pre-filled (WhatsApp must be installed). */
    fun prefillWhatsApp(phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * True zero-tap WhatsApp send — ONLY works if the user has enabled
     * OrionAccessibilityService in Settings > Accessibility (read that
     * file's warning first). If it's not enabled, this silently degrades
     * to prefillWhatsApp() so the user still gets a working message draft
     * instead of nothing.
     */
    fun autoSendWhatsApp(phoneNumber: String, message: String) {
        if (com.orion.app.accessibility.OrionAccessibilityService.isEnabled(context)) {
            com.orion.app.accessibility.OrionAccessibilityService.pendingAutoSend =
                com.orion.app.accessibility.OrionAccessibilityService.PendingAutoSend(
                    targetPackage = "com.whatsapp",
                    expectedSnippet = message.take(24)
                )
        }
        prefillWhatsApp(phoneNumber, message)
    }
}

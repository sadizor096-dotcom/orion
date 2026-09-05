package com.orion.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * ⚠️ READ THIS ENTIRE COMMENT BEFORE ENABLING THIS SERVICE ⚠️
 *
 * This is the only way to make WhatsApp sending truly zero-tap (SMS doesn't
 * need this — see SmsSender.kt, which sends directly with no UI at all).
 * WhatsApp has no public API for personal-account automation, so the only
 * option left is reading the screen and tapping Send ourselves — exactly
 * what apps like Tasker do.
 *
 * What turning this on actually means:
 *   - It can read the text/layout of EVERY app's screen, not just WhatsApp —
 *     there's no per-app scoping in the Android API itself; the scoping
 *     below (checking event.packageName) is enforced in OUR code, not by
 *     the OS.
 *   - It can tap anywhere on screen on your behalf.
 *   - Android shows a strongly-worded warning before the user can enable it
 *     in Settings > Accessibility — that's intentional.
 *   - If you ever publish this on Google Play, policy only allows
 *     Accessibility usage when it IS the app's core purpose — using it for
 *     a convenience feature risks rejection. Irrelevant for a personal,
 *     sideloaded build.
 *
 * How the auto-send actually works:
 *   1. Before opening WhatsApp, MessagingHelper.autoSendWhatsApp() stores a
 *      `pendingAutoSend` (target package + a snippet of the message).
 *   2. Once WhatsApp's window appears, onAccessibilityEvent fires.
 *   3. As a safety check, we only proceed if the expected message snippet
 *      is actually visible on screen — this stops us from tapping Send on
 *      the wrong chat if timing is off, or on some unrelated WhatsApp screen.
 *   4. We find the Send button and click it, then clear pendingAutoSend so
 *      nothing fires again until the next explicit request.
 */
class OrionAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pending = pendingAutoSend ?: return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != pending.targetPackage) return

        val root = rootInActiveWindow ?: return
        if (!containsText(root, pending.expectedSnippet)) return

        val sendNode = findSendButton(root) ?: return
        sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        pendingAutoSend = null // one-shot — must be re-armed for the next message
    }

    override fun onInterrupt() {}

    private fun containsText(node: AccessibilityNodeInfo, snippet: String): Boolean {
        val text = node.text?.toString().orEmpty()
        if (snippet.isNotBlank() && text.contains(snippet, ignoreCase = true)) return true
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (containsText(child, snippet)) return true
            }
        }
        return false
    }

    /**
     * WhatsApp exposes its send button with content description "Send" in
     * English locales only — this string changes with the device's
     * language. A real deployment should either check the device locale
     * and match the localized string, or better, use
     * findAccessibilityNodeInfosByViewId(...) with WhatsApp's actual
     * resource id (found once via Android Studio's Layout Inspector while
     * WhatsApp is running) instead of matching visible text, since resource
     * ids don't change with locale.
     */
    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString().orEmpty()
        if (desc.equals("Send", ignoreCase = true) && node.isClickable) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child -> findSendButton(child)?.let { return it } }
        }
        return null
    }

    data class PendingAutoSend(val targetPackage: String, val expectedSnippet: String)

    companion object {
        @Volatile
        var pendingAutoSend: PendingAutoSend? = null

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = ComponentName(context, OrionAccessibilityService::class.java).flattenToString()
            return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
        }
    }
}

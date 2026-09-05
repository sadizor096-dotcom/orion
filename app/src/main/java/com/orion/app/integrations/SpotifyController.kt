package com.orion.app.integrations

/**
 * Spotify playback control — READ THIS BEFORE WIRING IT UP.
 *
 * "Bir kere izin isteyip sonra otomatik kontrol" is genuinely achievable
 * for Spotify specifically, because Spotify publishes an official SDK for
 * exactly this — but it's not zero-setup:
 *
 *  1. You (the developer) must register an app at
 *     https://developer.spotify.com/dashboard and get a Client ID.
 *  2. Add Spotify's App Remote SDK (distributed via their own Maven repo,
 *     NOT Maven Central — see their GitHub docs for the current
 *     repository URL, it changes occasionally) as a dependency.
 *  3. The END USER authorizes ONCE (Spotify's standard OAuth screen) —
 *     after that, `SpotifyAppRemote.connect(...)` reconnects silently on
 *     every app launch as long as the Spotify app is installed and the
 *     user is logged in. That matches what you asked for.
 *  4. Requires the user to have the Spotify app installed (App Remote
 *     controls the installed app; it does not stream audio itself).
 *
 * Below is the real call shape once the SDK is added — left commented so
 * this file compiles without the extra dependency until you've done step 2:
 *
 * ```
 * import com.spotify.android.appremote.api.ConnectionParams
 * import com.spotify.android.appremote.api.Connector
 * import com.spotify.android.appremote.api.SpotifyAppRemote
 *
 * class SpotifyController(private val context: Context, private val clientId: String) {
 *     private var appRemote: SpotifyAppRemote? = null
 *
 *     fun connect(onReady: () -> Unit) {
 *         val params = ConnectionParams.Builder(clientId)
 *             .setRedirectUri("orion://callback")
 *             .showAuthView(true) // shown only the very first time
 *             .build()
 *         SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
 *             override fun onConnected(remote: SpotifyAppRemote) {
 *                 appRemote = remote
 *                 onReady()
 *             }
 *             override fun onFailure(error: Throwable) { }
 *         })
 *     }
 *
 *     fun resumeLastPlayed() { appRemote?.playerApi?.resume() }
 *
 *     fun playByName(query: String) {
 *         // Full "play THIS song by name" needs Spotify's Web API search
 *         // endpoint (separate OAuth token, same one-time login) to resolve
 *         // a track URI, then: appRemote?.playerApi?.play(trackUri)
 *     }
 * }
 * ```
 *
 * Until the SDK is wired in, the honest fallback below opens the Spotify
 * app directly (real, works today, zero extra setup) — it just can't pick
 * a specific song by name, only hand off to whatever Spotify last had
 * queued.
 */
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object SpotifyFallback {
    fun openAndResume(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun isSpotifyInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo("com.spotify.music", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

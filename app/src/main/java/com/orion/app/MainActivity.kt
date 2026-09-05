package com.orion.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orion.app.core.OrionViewModel
import com.orion.app.ui.screens.MainScreen
import com.orion.app.ui.theme.OrionTheme

/**
 * Single-activity Compose host. All real navigation happens inside MainScreen —
 * there is no WebView anywhere in this app; every panel is native Compose.
 */
class MainActivity : ComponentActivity() {

    private var micGranted = mutableStateOf(false)

    // One-time permission requests for the automation features — after the
    // user grants these once, SmsSender / MessagingHelper / weather all work
    // silently with no further prompts.
    private val multiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        micGranted.value = results[Manifest.permission.RECORD_AUDIO] ?: micGranted.value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        micGranted.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        requestRemainingPermissions()

        setContent {
            val viewModel: OrionViewModel = viewModel(
                factory = OrionViewModel.factory(applicationContext)
            )
            val granted by micGranted

            OrionTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        micPermissionGranted = granted,
                        onRequestMicPermission = {
                            multiPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    )
                }
            }
        }
    }

    private fun requestRemainingPermissions() {
        val needed = mutableListOf<String>()
        val check = { p: String -> ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED }

        if (!check(Manifest.permission.RECORD_AUDIO)) needed += Manifest.permission.RECORD_AUDIO
        if (!check(Manifest.permission.ACCESS_FINE_LOCATION)) needed += Manifest.permission.ACCESS_FINE_LOCATION
        if (!check(Manifest.permission.READ_CONTACTS)) needed += Manifest.permission.READ_CONTACTS
        if (!check(Manifest.permission.SEND_SMS)) needed += Manifest.permission.SEND_SMS

        if (needed.isNotEmpty()) multiPermissionLauncher.launch(needed.toTypedArray())
    }
}

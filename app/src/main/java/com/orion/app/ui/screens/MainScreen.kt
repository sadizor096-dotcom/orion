package com.orion.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.core.CoreState
import com.orion.app.core.OrionViewModel
import com.orion.app.ui.components.AiCoreView
import com.orion.app.ui.components.ChatPanel
import com.orion.app.ui.components.NavigationRail
import com.orion.app.ui.components.SystemHudPanel
import com.orion.app.ui.theme.CoreBlue
import com.orion.app.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: OrionViewModel,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit
) {
    var showBoot by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2200)
        showBoot = false
    }

    LaunchedEffect(micPermissionGranted) {
        if (micPermissionGranted) viewModel.startClapListening() else onRequestMicPermission()
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // Three-pane landscape layout: nav ~18% / AI Core ~57% / HUD ~25%
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.weight(0.18f))

            CoreColumn(viewModel = viewModel, modifier = Modifier.weight(0.57f))

            val hud by viewModel.hud.collectAsState()
            val isListening by viewModel.isListeningForClaps.collectAsState()
            SystemHudPanel(
                hud = hud,
                isClapListening = isListening,
                modifier = Modifier.weight(0.25f)
            )
        }

        AnimatedVisibility(
            visible = showBoot,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(500))
        ) {
            BootSequence()
        }
    }
}

@Composable
private fun CoreColumn(viewModel: OrionViewModel, modifier: Modifier = Modifier) {
    val coreState by viewModel.coreState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val isAwake by viewModel.isAwake.collectAsState()
    val activeAlert by viewModel.activeAlertMessage.collectAsState()

    Column(modifier = modifier.fillMaxHeight().padding(20.dp)) {
        Text(
            text = if (!isAwake) "STANDBY — UYKU MODU" else stateLabel(coreState),
            color = if (coreState == CoreState.ALERT) com.orion.app.ui.theme.AlertRed else CoreBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (activeAlert != null) {
            Text(
                text = activeAlert ?: "",
                color = com.orion.app.ui.theme.AlertRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
                    .clickable { viewModel.acknowledgeAlert() }
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AiCoreView(
                state = coreState,
                modifier = Modifier.fillMaxHeight(0.95f),
                isCharging = isCharging,
                isAwake = isAwake
            )
        }

        Spacer(Modifier.height(12.dp))

        ChatPanel(
            messages = messages,
            onSend = { viewModel.sendMessage(it) },
            onMicClick = { viewModel.activateVoiceInput() },
            isListening = coreState == CoreState.LISTENING,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}

private fun stateLabel(state: CoreState): String = when (state) {
    CoreState.IDLE -> "STANDBY"
    CoreState.LISTENING -> "LISTENING"
    CoreState.THINKING -> "ANALYZING"
    CoreState.SEARCHING -> "SCANNING"
    CoreState.EXECUTING -> "EXECUTING"
    CoreState.COMPLETE -> "TARGET ACQUIRED"
    CoreState.ALERT -> "⚠ ALERT"
}

/** Short premium boot sequence: constellation -> core activation -> ONLINE. */
@Composable
private fun BootSequence() {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(600); phase = 1
        delay(700); phase = 2
    }
    Box(modifier = Modifier.fillMaxSize().background(VoidBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (phase) {
                    0 -> "ORION CONSTELLATION"
                    1 -> "AI CORE ACTIVATION"
                    else -> "O.R.I.O.N.\nONLINE"
                },
                color = CoreBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

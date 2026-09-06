package com.orion.app.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.core.HudState
import com.orion.app.ui.theme.*

@Composable
fun SystemHudPanel(hud: HudState, isClapListening: Boolean, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxHeight().background(PanelDark).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { OrionStatusCard(hud, isClapListening) }
        item { HudCard(title = "🕐 TIME") { ClockBlock() } }
        item {
            HudCard(title = "💻 CPU / GPU") {
                MetricRow("CPU", if (hud.cpu?.available == true) "Readable" else "DATA UNAVAILABLE", ok = hud.cpu?.available == true)
                MetricRow("GPU", "DATA UNAVAILABLE", ok = false)
            }
        }
        item {
            HudCard(title = "🧠 RAM") {
                if (hud.ram != null) {
                    MetricBar(label = "RAM", percent = hud.ram.percent, sub = "${hud.ram.usedMb} MB / ${hud.ram.totalMb} MB")
                } else {
                    MetricRow("RAM", "DATA UNAVAILABLE", ok = false)
                }
            }
        }
        item {
            HudCard(title = "🗄️ STORAGE") {
                if (hud.storage != null) {
                    val used = String.format("%.1f", hud.storage.usedGb)
                    val total = String.format("%.1f", hud.storage.totalGb)
                    MetricBar(label = "STORAGE", percent = hud.storage.percent, sub = "$used GB / $total GB")
                } else {
                    MetricRow("STORAGE", "DATA UNAVAILABLE", ok = false)
                }
            }
        }
        item {
            HudCard(title = "🔋 BATTERY") {
                if (hud.battery != null && hud.battery.percent >= 0) {
                    BatteryIconRow(percent = hud.battery.percent, charging = hud.battery.isCharging)
                    MetricRow("LEVEL", "${hud.battery.percent}%", ok = true)
                    MetricRow("CHARGING", if (hud.battery.isCharging) "Yes ⚡" else "No", ok = true)
                } else {
                    MetricRow("BATTERY", "DATA UNAVAILABLE", ok = false)
                }
            }
        }
        item {
            HudCard(title = "📡 NETWORK") {
                if (hud.network != null) {
                    MetricRow("TRANSPORT", hud.network.transport, ok = hud.network.connected)
                    MetricRow(
                        "DOWNLINK",
                        hud.network.downlinkKbps?.let { "${it / 1000} Mbps (est.)" } ?: "Unavailable",
                        ok = hud.network.downlinkKbps != null
                    )
                    MetricRow(
                        "LATENCY",
                        hud.latencyMs?.let { "$it ms (measured)" } ?: "Unavailable",
                        ok = hud.latencyMs != null
                    )
                } else {
                    MetricRow("NETWORK", "DATA UNAVAILABLE", ok = false)
                }
            }
        }
    }
}

@Composable
private fun OrionStatusCard(hud: HudState, isClapListening: Boolean) {
    HudCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(ok = true)
            Spacer(Modifier.width(6.dp))
            Text("ORION ONLINE", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        MetricRow("BACKEND", if (hud.backendReachable) "Connected" else "Unreachable", ok = hud.backendReachable)
        MetricRow("MIC", if (isClapListening) "Listening" else "Idle", ok = true)
        MetricRow("TOOLS", "7 Available", ok = true)
    }
}

@Composable
private fun ClockBlock() {
    var timeText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var dateText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance()
            timeText = String.format("%02d:%02d", now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE))
            val sdf = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("tr"))
            dateText = sdf.format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }
    Text(timeText, color = InkPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Text(dateText, color = InkDim, fontSize = 11.sp)
}

@Composable
private fun HudCard(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33101823))
            .padding(12.dp)
    ) {
        if (title != null) {
            Text(title, color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
        }
        content()
    }
}

@Composable
private fun MetricRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = InkFaint, fontSize = 10.sp)
        Text(value, color = if (ok) InkPrimary else IonAmber, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricBar(label: String, percent: Int, sub: String) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = InkDim, fontSize = 10.sp)
            Text("$percent%", color = InkPrimary, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x1AFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .background(Brush.horizontalGradient(listOf(CoreBlueDim, CoreBlue)))
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(sub, color = InkFaint, fontSize = 9.sp)
    }
}

@Composable
private fun BatteryIconRow(percent: Int, charging: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x22FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .background(if (percent < 20) AlertRed else StatusGreen)
            )
            // A plain black bolt drawn over the fill — visible against both the
            // empty (dark) and filled (green/red) portions of the icon.
            if (charging) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Bolt,
                    contentDescription = "Charging",
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("${percent}%", color = InkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusDot(ok: Boolean) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(RoundedCornerShape(50))
            .background(if (ok) StatusGreen else AlertRed)
    )
}

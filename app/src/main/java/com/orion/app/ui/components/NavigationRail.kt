package com.orion.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.ui.theme.CoreBlue
import com.orion.app.ui.theme.InkDim
import com.orion.app.ui.theme.LineFaint
import com.orion.app.ui.theme.PanelDark

private data class NavEntry(val label: String, val icon: ImageVector)

private val navEntries = listOf(
    NavEntry("New Chat", Icons.Default.Add),
    NavEntry("History", Icons.Default.History),
    NavEntry("Projects", Icons.Default.Folder),
    NavEntry("Memory", Icons.Default.Psychology),
    NavEntry("Research", Icons.Default.Search),
    NavEntry("Code", Icons.Default.Code),
    NavEntry("Learn", Icons.Default.School),
    NavEntry("Files", Icons.Default.InsertDriveFile),
    NavEntry("Settings", Icons.Default.Settings)
)

@Composable
fun NavigationRail(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf("New Chat") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(PanelDark)
            .padding(vertical = 20.dp, horizontal = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
            Text("O.R.I.O.N.", color = CoreBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(navEntries) { entry ->
                val isSelected = entry.label == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = entry.label }
                        .background(
                            if (isSelected) CoreBlue.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        entry.icon,
                        contentDescription = entry.label,
                        tint = if (isSelected) CoreBlue else InkDim,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        entry.label,
                        color = if (isSelected) androidx.compose.ui.graphics.Color(0xFFEAF6FF) else InkDim,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text("BUILD 2026.08 · TABLET", color = InkDim, fontSize = 9.sp)
    }
}

package com.orion.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.app.core.ChatMessage
import com.orion.app.ui.theme.*

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(msg)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x14FFFFFF))
                .padding(6.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask O.R.I.O.N...", color = InkFaint) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                ),
                singleLine = true
            )
            IconButton(onClick = onMicClick) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = if (isListening) CoreBlue else InkDim
                )
            }
            IconButton(onClick = {
                if (input.isNotBlank()) { onSend(input); input = "" }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = CoreBlue)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isUser) Color(0x185EC7FF) else Color(0x0AFFFFFF))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isUser) {
                Text("O.R.I.O.N.", color = CoreBlueDim, fontSize = 9.sp)
                Spacer(Modifier.height(2.dp))
            }
            Text(msg.text, color = InkPrimary, fontSize = 13.5.sp)
        }
    }
}

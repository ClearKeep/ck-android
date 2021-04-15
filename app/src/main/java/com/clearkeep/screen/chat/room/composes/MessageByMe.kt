package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun MessageByMe(messageDisplayInfo: MessageDisplayInfo) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
            style = MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                color = grayscale3,
                textAlign = TextAlign.Start
            ),
            modifier = Modifier.weight(1.0f, true),
        )
        Column(
            modifier = Modifier.weight(2.0f, true),
            horizontalAlignment = Alignment.End
        ) {
            Card (
                backgroundColor = grayscale2,
                shape = messageDisplayInfo.cornerShape,
            ) {
                Text(
                    text = messageDisplayInfo.message.message,
                    style = MaterialTheme.typography.body2.copy(
                        color = grayscaleOffWhite
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}
package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun MessageFromOther(message: Message, username: String, isGroup: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isGroup) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.caption,
                )
                Text(
                    text = getHourTimeAsString(message.createdTime),
                    style = MaterialTheme.typography.caption.copy(
                        fontWeight = FontWeight.Medium,
                        color = grayscale3,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.weight(1.0f, true),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(2.0f, true),
            ) {
                RoundCornerMessage(
                    message = message.message,
                    backgroundColor = primaryDefault,
                    textColor = grayscaleOffWhite,
                )
            }
            Column(
                modifier = Modifier.weight(1.0f, true),
                horizontalAlignment = Alignment.End
            ) {
                if (!isGroup) {
                    Text(
                        text = getHourTimeAsString(message.createdTime),
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Medium,
                            color = grayscale3,
                            textAlign = TextAlign.End
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundCornerMessage(message: String, backgroundColor: Color, textColor: Color) {
    Card (
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = roundSize,
            bottomEnd = roundSize,
            bottomStart = roundSize
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body2.copy(
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}
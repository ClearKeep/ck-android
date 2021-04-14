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
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.utilities.getHourTimeAsString

val roundSize = 24.dp

@Composable
fun MessageByMe(message: Message) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = getHourTimeAsString(message.createdTime),
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
            RoundCornerMessage(
                message = message.message,
                backgroundColor = grayscale2,
                textColor = Color.White,
            )
        }
    }
}

@Composable
private fun RoundCornerMessage(message: String, backgroundColor: Color, textColor: Color) {
    Card (
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(
            topStart = roundSize,
            topEnd = roundSize,
            bottomEnd = 0.dp,
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
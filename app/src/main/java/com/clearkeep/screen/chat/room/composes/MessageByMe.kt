package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun MessageByMe(messageDisplayInfo: MessageDisplayInfo) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.dp else 2.dp))
        Text(
            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
            style = MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                color = grayscale3,
                textAlign = TextAlign.Start
            ),
        )
        Card(
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

@Preview
@Composable
fun MessageByMePreview() {
    MessageByMe(
        MessageDisplayInfo(
            Message(null, "", 0, "", "", "", "msg", 0, 0, "", ""),
            true,
            false,
            true,
            "linh",
            RoundedCornerShape(8.dp)
        )
    )
}
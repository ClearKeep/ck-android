package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.clearkeep.utilities.printlnCK

@ExperimentalFoundationApi
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
            if (isImageMessage(messageDisplayInfo.message.message)) {
                ImageMessageContent(Modifier.padding(24.dp, 16.dp), getImageUriStrings(messageDisplayInfo.message.message))
            } else {
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

private fun isImageMessage(content: String) : Boolean {
    return content.contains(imageMessageRegex)
}

private fun getImageUriStrings(content: String) : List<String> {
    return imageMessageRegex.findAll(content).map {
        it.value.split(" ")
    }.toList().flatten()
}

private val imageMessageRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/dev.+[a-zA-Z0-9\\/\\_\\-\\.]+(\\.png|\\.jpeg|\\.jpg|\\.gif|\\.PNG|\\.JPEG|\\.JPG|\\.GIF))".toRegex()
package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@ExperimentalFoundationApi
@Composable
fun MessageByMe(messageDisplayInfo: MessageDisplayInfo) {
    Column(Modifier.fillMaxWidth().wrapContentHeight(), horizontalAlignment = Alignment.End) {
        Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.dp else 2.dp))
        Text(
            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
            style = MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                color = grayscale3,
                textAlign = TextAlign.Start
            ),
        )
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (messageDisplayInfo.message.message.contains(tempImageRegex))
                CircularProgressIndicator(Modifier.size(20.dp), grayscale1, 2.dp)
            Spacer(Modifier.width(18.dp))
            Card(
                backgroundColor = grayscale2,
                shape = messageDisplayInfo.cornerShape,
            ) {
                Column {
                    if (isImageMessage(messageDisplayInfo.message.message)) {
                        ImageMessageContent(
                            Modifier.padding(24.dp, 16.dp),
                            getImageUriStrings(messageDisplayInfo.message.message)
                        )
                    }
                    val messageContent = getMessageContent(messageDisplayInfo.message.message)
                    if (messageContent.isNotBlank()) {
                        Row(Modifier.align(Alignment.End).wrapContentHeight()) {
                            Text(
                                text = messageContent,
                                style = MaterialTheme.typography.body2.copy(
                                    color = grayscaleOffWhite
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isImageMessage(content: String): Boolean {
    return content.contains(remoteImageRegex) || content.contains(tempImageRegex)
}

private fun getImageUriStrings(content: String): List<String> {
    val temp = remoteImageRegex.findAll(content).map {
        it.value.split(" ")
    }.toMutableList()
    temp.add(tempImageRegex.findAll(content).map { it.value.split(" ") }.toList().flatten())
    return temp.flatten()
}

private fun getMessageContent(content: String): String {
    val temp = remoteImageRegex.replace(content, "")
    return tempImageRegex.replace(temp, "")
}

private val remoteImageRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/dev.+[a-zA-Z0-9\\/\\_\\-\\.]+(\\.png|\\.jpeg|\\.jpg|\\.gif|\\.PNG|\\.JPEG|\\.JPG|\\.GIF))".toRegex()

private val tempImageRegex =
    "content://media/external/images/media/\\d+".toRegex()
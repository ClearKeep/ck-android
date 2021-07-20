package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.components.*
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun MessageFromOther(messageDisplayInfo: MessageDisplayInfo) {
    Column {
        Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.dp else 2.dp ))
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.width(26.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (messageDisplayInfo.showAvatarAndName) {
                    CircleAvatar(emptyList(), messageDisplayInfo.userName, size = 18.dp)
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (messageDisplayInfo.showAvatarAndName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = messageDisplayInfo.userName,
                            style = MaterialTheme.typography.body2.copy(
                                color = colorSuccessDefault,
                                fontWeight = FontWeight.W600
                            ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                            style = MaterialTheme.typography.caption.copy(
                                fontWeight = FontWeight.Medium,
                                color = grayscale3,
                                textAlign = TextAlign.Start
                            ),
                            modifier = Modifier
                                .weight(1.0f, true)
                                .padding(start = 4.dp),
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (!messageDisplayInfo.showAvatarAndName) {
                            Text(
                                text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                                style = MaterialTheme.typography.caption.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = grayscale3,
                                    textAlign = TextAlign.End
                                ),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                backgroundColor = primaryDefault,
                                shape = messageDisplayInfo.cornerShape,
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    if (isImageMessage(messageDisplayInfo.message.message)) {
                                        ImageMessageContent(
                                            Modifier.padding(24.dp, 16.dp),
                                            getImageUriStrings(messageDisplayInfo.message.message)
                                        )
                                    }
                                    val messageContent =
                                        getMessageContent(messageDisplayInfo.message.message)
                                    if (messageContent.isNotBlank()) {
                                        Row(Modifier.align(Alignment.Start).wrapContentHeight()) {
                                            ClickableLinkContent(messageContent)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            if (messageDisplayInfo.message.message.contains(tempImageRegex))
                            CircularProgressIndicator(Modifier.size(20.dp), grayscale1, 2.dp)
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
    "(https://s3.amazonaws.com/storage.clearkeep.io/[dev|prod].+[a-zA-Z0-9\\/\\_\\-\\.]+(\\.png|\\.jpeg|\\.jpg|\\.gif|\\.PNG|\\.JPEG|\\.JPG|\\.GIF))".toRegex()

private val tempImageRegex =
    "content://media/external/images/media/\\d+".toRegex()
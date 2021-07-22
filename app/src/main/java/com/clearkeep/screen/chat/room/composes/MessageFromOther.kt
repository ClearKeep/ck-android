package com.clearkeep.screen.chat.room.composes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.clearkeep.components.*
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.getHourTimeAsString
import com.clearkeep.utilities.*

@Composable
fun MessageFromOther(messageDisplayInfo: MessageDisplayInfo) {
    val message = messageDisplayInfo.message.message
    val context = LocalContext.current
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
                                    if (isImageMessage(message)) {
                                        ImageMessageContent(
                                            Modifier.padding(24.dp, 16.dp),
                                            getImageUriStrings(message)
                                        )
                                    } else if (isFileMessage(message)) {
                                        FileMessageContent(getFileUriStrings(message)) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                            ContextCompat.startActivity(context, intent, null)
                                        }
                                    }
                                    val messageContent = getMessageContent(message)
                                    if (messageContent.isNotBlank()) {
                                        Row(Modifier.align(Alignment.Start).wrapContentHeight()) {
                                            ClickableLinkContent(messageContent)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            if (isTempMessage(message))
                                CircularProgressIndicator(Modifier.size(20.dp), grayscale1, 2.dp)
                        }
                    }
                }
            }
        }
    }
}
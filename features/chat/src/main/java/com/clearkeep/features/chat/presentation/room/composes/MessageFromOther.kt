package com.clearkeep.features.chat.presentation.room.composes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.common.utilities.*
import com.clearkeep.features.chat.presentation.composes.CircleAvatar
import com.clearkeep.features.chat.presentation.room.messagedisplaygenerator.MessageDisplayInfo

@Composable
fun MessageFromOther(
    messageDisplayInfo: MessageDisplayInfo,
    onClickFile: (url: String) -> Unit,
    onClickImage: (uris: List<String>, senderName: String) -> Unit,
    onLongClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit,
    onQuoteClick: (messageDisplayInfo: MessageDisplayInfo) ->Unit
) {
    val message = messageDisplayInfo.message.message

    if (message.isNotBlank()) {
        Column {
            Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.sdp() else 2.sdp()))
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(26.sdp()),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (messageDisplayInfo.showAvatarAndName) {
                        CircleAvatar(
                            arrayListOf(messageDisplayInfo.avatar),
                            messageDisplayInfo.userName,
                            size = 18.sdp()
                        )
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (messageDisplayInfo.showAvatarAndName) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (messageDisplayInfo.isForwardedMessage) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(4.sdp())
                                        .background(grayscale2, RoundedCornerShape(8.sdp()))
                                )
                                Spacer(Modifier.width(4.sdp()))
                                Text(
                                    text = stringResource(
                                        R.string.forwarded_message,
                                        messageDisplayInfo.userName
                                    ),
                                    style = MaterialTheme.typography.caption.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = grayscale3,
                                        textAlign = TextAlign.Start,
                                        fontSize = defaultNonScalableTextSize()
                                    ),
                                )
                            } else {
                                Text(
                                    text = messageDisplayInfo.userName,
                                    style = MaterialTheme.typography.body2.copy(
                                        color = colorSuccessDefault,
                                        fontWeight = FontWeight.W600,
                                        fontSize = defaultNonScalableTextSize()
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.sdp()))
                            Text(
                                text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                                style = MaterialTheme.typography.caption.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = grayscale3,
                                    textAlign = TextAlign.Start,
                                    fontSize = defaultNonScalableTextSize()
                                ),
                                modifier = Modifier
                                    .weight(1.0f, true)
                                    .padding(start = 4.sdp()),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                        ) {
                            if (messageDisplayInfo.isQuoteMessage) {
                                QuotedMessageView(messageDisplayInfo) {
                                    onQuoteClick(messageDisplayInfo)
                                }
                            }

                            Row(Modifier.height(IntrinsicSize.Max)) {
                                if (!messageDisplayInfo.showAvatarAndName) {
                                    if (messageDisplayInfo.isForwardedMessage) {
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .width(4.sdp())
                                                .background(grayscale2, RoundedCornerShape(8.sdp()))
                                        )
                                        Spacer(Modifier.width(4.sdp()))
                                        Text(
                                            text = stringResource(
                                                R.string.forwarded_message,
                                                messageDisplayInfo.userName
                                            ),
                                            style = MaterialTheme.typography.caption.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = grayscale3,
                                                textAlign = TextAlign.Start,
                                                fontSize = defaultNonScalableTextSize()
                                            ),
                                        )
                                        Spacer(Modifier.width(4.sdp()))
                                        CKText(
                                            text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                                            style = MaterialTheme.typography.caption.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = grayscale3,
                                                textAlign = TextAlign.End,
                                                fontSize = defaultNonScalableTextSize()
                                            ),
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    Modifier.pointerInput(messageDisplayInfo.message.hashCode()) {
                                        detectTapGestures(
                                            onLongPress = {
                                                printlnCK("Long press on message ${messageDisplayInfo.message.message}")
                                                onLongClick(messageDisplayInfo)
                                            }
                                        )
                                    },
                                    backgroundColor = primaryDefault,
                                    shape = messageDisplayInfo.cornerShape,
                                ) {
                                    Row(Modifier.height(IntrinsicSize.Max)) {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            if (isImageMessage(message)) {
                                                ImageMessageContent(
                                                    Modifier.padding(24.sdp(), 16.sdp()),
                                                    getImageUriStrings(message),
                                                    false
                                                ) {
                                                    onClickImage.invoke(
                                                        getImageUriStrings(message),
                                                        messageDisplayInfo.userName
                                                    )
                                                }
                                            } else if (isFileMessage(message)) {
                                                FileMessageContent(getFileUriStrings(message), false) {
                                                    onClickFile.invoke(it)
                                                }
                                            }
                                            val messageContent = getMessageContent(message)
                                            if (messageContent.isNotBlank()) {
                                                Row(
                                                    Modifier
                                                        .align(Alignment.Start)
                                                        .wrapContentHeight()
                                                ) {
                                                    ClickableLinkContent(
                                                        messageContent,
                                                        false,
                                                        messageDisplayInfo.message.hashCode()
                                                    ) {
                                                        onLongClick(messageDisplayInfo)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(18.sdp()))
                                if (isTempMessage(message))
                                    CircularProgressIndicator(
                                        Modifier.size(20.sdp()),
                                        grayscale1,
                                        2.sdp()
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}
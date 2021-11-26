package com.clearkeep.features.chat.presentation.room.composes

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.grayscale1
import com.clearkeep.common.presentation.components.grayscale2
import com.clearkeep.common.presentation.components.grayscale3
import com.clearkeep.common.utilities.*
import com.clearkeep.features.chat.presentation.room.messagedisplaygenerator.MessageDisplayInfo

@ExperimentalFoundationApi
@Composable
fun MessageByMe(
    messageDisplayInfo: MessageDisplayInfo,
    onClickFile: (uri: String) -> Unit,
    onClickImage: (uris: List<String>, senderName: String) -> Unit,
    onLongClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit
) {
    val message = messageDisplayInfo.message.message

    if (message.isNotBlank()) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.End
        ) {
            Spacer(modifier = Modifier.height(if (messageDisplayInfo.showSpacer) 8.sdp() else 2.sdp()))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
                if (messageDisplayInfo.isForwardedMessage) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(4.sdp())
                            .background(grayscale2, RoundedCornerShape(8.sdp()))
                    )
                    Spacer(Modifier.width(4.sdp()))
                    val forwardedMessageSender = stringResource(R.string.forwarded_message_from_you)
                    Text(
                        text = stringResource(R.string.forwarded_message, forwardedMessageSender),
                        modifier = Modifier.fillMaxHeight(),
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Medium,
                            color = grayscale3,
                            textAlign = TextAlign.Start,
                            fontSize = defaultNonScalableTextSize()
                        ),
                    )
                }
                Spacer(Modifier.width(4.sdp()))
                Text(
                    text = getHourTimeAsString(messageDisplayInfo.message.createdTime),
                    modifier = Modifier.fillMaxHeight(),
                    style = MaterialTheme.typography.caption.copy(
                        fontWeight = FontWeight.Medium,
                        color = grayscale3,
                        textAlign = TextAlign.Start,
                        fontSize = defaultNonScalableTextSize()
                    ),
                )
            }
            if (messageDisplayInfo.isQuoteMessage) {
                QuotedMessageView(messageDisplayInfo)
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTempMessage(message))
                    CircularProgressIndicator(Modifier.size(20.sdp()), grayscale1, 2.sdp())
                Spacer(Modifier.width(18.sdp()))
                Card(
                    Modifier.pointerInput(messageDisplayInfo.message.hashCode()) {
                        detectTapGestures(
                            onLongPress = {
                                printlnCK("Long press on message ${messageDisplayInfo.message.message}")
                                onLongClick(messageDisplayInfo)
                            }
                        )
                    },
                    backgroundColor = grayscale2,
                    shape = messageDisplayInfo.cornerShape,
                ) {
                    ConstraintLayout {
                        val (messageContent, quotedMessageIndicator) = createRefs()

                        Column(Modifier.constrainAs(messageContent) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            end.linkTo(quotedMessageIndicator.start)
                            height = Dimension.wrapContent
                            width = Dimension.wrapContent
                        }, horizontalAlignment = Alignment.End) {
                            if (isImageMessage(message)) {
                                ImageMessageContent(
                                    Modifier.padding(24.sdp(), 16.sdp()),
                                    getImageUriStrings(message),
                                    false
                                ) {
                                    onClickImage.invoke(getImageUriStrings(message), "You")
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
                                        .align(Alignment.End)
                                        .wrapContentHeight()
                                        .pointerInput(messageDisplayInfo.message.hashCode()) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    onLongClick(messageDisplayInfo)
                                                }
                                            )
                                        }) {
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
            }
        }
    }
}
package com.clearkeep.presentation.screen.chat.room.composes

import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.rememberImagePainter
import coil.imageLoader
import com.clearkeep.R
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.common.presentation.components.base.CKTextInputFieldChat
import com.clearkeep.common.utilities.isFileMessage
import com.clearkeep.common.utilities.isImageMessage
import com.clearkeep.presentation.screen.chat.room.RoomViewModel
import com.clearkeep.common.utilities.sdp

@Composable
fun SendBottomCompose(
    roomViewModel: RoomViewModel,
    onSendMessage: (String) -> Unit,
    onClickUploadPhoto: () -> Unit,
    onClickUploadFile: () -> Unit
) {
    val msgState = roomViewModel.message.observeAsState()
    val selectedImagesList = roomViewModel.imageUriSelected.observeAsState()
    val quotedMessage = roomViewModel.quotedMessage.observeAsState()

    Column(Modifier.background(MaterialTheme.colors.background)) {
        if (quotedMessage.value != null) {
            ConstraintLayout(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.sdp())) {
                val (reply, close) = createRefs()

                val quotedMessageSender =
                    if (quotedMessage.value!!.isOwner) "You" else quotedMessage.value!!.userName
                CKText(
                    stringResource(R.string.replying_to, quotedMessageSender),
                    Modifier
                        .constrainAs(reply) {
                            top.linkTo(close.top)
                            bottom.linkTo(close.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(close.end)
                            width = Dimension.fillToConstraints
                        },
                    color = LocalColorMapping.current.bodyTextDisabled
                )
                val iconMargin = 10.sdp()
                Icon(painterResource(R.drawable.ic_cross),
                    null,
                    Modifier
                        .padding(vertical = 8.sdp())
                        .size(28.sdp())
                        .clickable {
                            roomViewModel.clearQuoteMessage()
                        }
                        .constrainAs(close) {
                            end.linkTo(parent.end, iconMargin)
                            top.linkTo(parent.top)
                        },
                    tint = LocalColorMapping.current.bodyTextDisabled
                )
            }

            Row(
                Modifier
                    .padding(horizontal = 14.sdp())
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(4.sdp())
                        .background(grayscale2, RoundedCornerShape(8.sdp()))
                )
                Spacer(Modifier.width(16.sdp()))
                val quote =
                    when {
                        isImageMessage(quotedMessage.value!!.message.message) -> {
                            "Image"
                        }
                        isFileMessage(quotedMessage.value!!.message.message) -> {
                            "File"
                        }
                        else -> {
                            quotedMessage.value!!.message.message
                        }
                    }
                CKText(
                    quote,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalColorMapping.current.bodyTextDisabled
                )
            }
        }

        selectedImagesList.value.let { values ->
            if (!values.isNullOrEmpty())
                Row(
                    Modifier
                        .padding(horizontal = 14.sdp())
                        .fillMaxWidth()
                ) {
                    LazyRow {
                        itemsIndexed(values) { _, uri ->
                            ItemImage(uri) {
                                roomViewModel.removeImage(it)
                            }
                            Spacer(modifier = Modifier.width(8.sdp()))
                        }
                    }
                }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = MaterialTheme.colors.background)
        ) {
            IconButton(
                onClick = {
                    onClickUploadPhoto()
                },
                modifier = Modifier
                    .padding(8.sdp())
                    .size(24.sdp()),
            ) {

                Icon(
                    painterResource(R.drawable.ic_photos),
                    contentDescription = "",
                    tint = LocalColorMapping.current.descriptionTextAlt,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onClickUploadPhoto()
                        }
                )
            }

            IconButton(
                onClick = {
                    onClickUploadFile()
                },
                modifier = Modifier
                    .padding(8.sdp())
                    .size(24.sdp()),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = "",
                    tint = LocalColorMapping.current.descriptionTextAlt,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.sdp(), top = 4.sdp(), bottom = 4.sdp())
            ) {
                CKTextInputFieldChat(
                    stringResource(R.string.message_input_hint),
                    msgState,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None,
                    onChangeMessage = {
                        roomViewModel.setMessage(it)
                    },
                    maxLines = 3
                )
            }
            IconButton(
                onClick = {
                    if (!TextUtils.isEmpty(msgState.value) || !selectedImagesList.value.isNullOrEmpty()) {
                        val message = msgState.value
                        onSendMessage(message ?: "")
                        // clear text
                        roomViewModel.setMessage("")
                    }
                },
                modifier = Modifier
                    .padding(8.sdp())
                    .width(24.sdp())
                    .height(24.sdp()),
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_send_plane
                    ),
                    contentDescription = "",
                    tint = MaterialTheme.colors.surface,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ItemImage(uri: String, onRemove: (uri: String) -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .size(56.sdp())
    ) {
        Image(
            painter = rememberImagePainter(
                uri,
                imageLoader = context.imageLoader,
            ), contentDescription = "", contentScale = ContentScale.Crop,
            modifier = Modifier.clip(RoundedCornerShape(16.sdp()))

        )
        Box(
            Modifier
                .size(16.sdp())
                .clip(CircleShape)
                .background(primaryDefault, CircleShape)
                .clickable {
                    onRemove(uri)
                }
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cross),
                null,
                tint = grayscaleOffWhite,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
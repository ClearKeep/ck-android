package com.clearkeep.screen.chat.room.composes

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.rememberImagePainter
import coil.imageLoader
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKTextInputFieldChat
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.sdp

@Composable
fun SendBottomCompose(
    roomViewModel: RoomViewModel,
    onSendMessage: (String) -> Unit,
    onClickUploadPhoto: () -> Unit,
    onClickUploadFile: () -> Unit
) {
    val msgState = roomViewModel.message.observeAsState()
    val selectedImagesList = roomViewModel.imageUriSelected.observeAsState()
    val isNote = roomViewModel.isNote.observeAsState()

    Column(Modifier.background(grayscaleBackground)) {
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
                .align(Alignment.TopEnd)) {
            Icon(
                painter = painterResource(R.drawable.ic_cross),
                null,
                tint = grayscaleOffWhite,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
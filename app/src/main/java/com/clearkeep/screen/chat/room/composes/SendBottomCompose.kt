package com.clearkeep.screen.chat.room.composes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import coil.imageLoader
import com.clearkeep.R
import com.clearkeep.components.base.CKTextInputFieldChat
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscaleBackground
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.components.primaryDefault
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.sdp
import com.google.accompanist.coil.rememberCoilPainter

@ExperimentalComposeUiApi
@Composable
fun SendBottomCompose(
    roomViewModel: RoomViewModel,
    navController: NavController,
    onSendMessage: (String) -> Unit,
    onClickUploadPhoto: () -> Unit,
    onClickUploadFile: () -> Unit
) {
    val msgState = roomViewModel.message.observeAsState()
    val selectedImagesList = roomViewModel.imageUriSelected.observeAsState()
    val isNote = roomViewModel.isNote.observeAsState()
    val context = LocalContext.current

    Column(Modifier.background(grayscaleBackground)) {
        selectedImagesList.value.let { values ->
            if (!values.isNullOrEmpty())
                Row(
                    Modifier
                        .padding(horizontal = 14.sdp())
                        .fillMaxWidth()) {
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
            modifier = Modifier.background(color = grayscaleBackground)
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
                    tint = grayscale1,
                    modifier = Modifier.fillMaxSize().clickable {
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
                    tint = grayscale1,
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
            painter = rememberCoilPainter(
                uri,
                imageLoader = context.imageLoader,
                previewPlaceholder = R.drawable.ic_cross
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
            Icon(painter = painterResource(R.drawable.ic_cross), null, tint = grayscaleOffWhite, modifier = Modifier.fillMaxSize()
            )
        }
    }
}
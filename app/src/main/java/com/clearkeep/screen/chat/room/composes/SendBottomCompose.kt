package com.clearkeep.screen.chat.room.composes

import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKTextInputFieldChat
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscaleBackground

@ExperimentalComposeUiApi
@Composable
fun SendBottomCompose(
    onSendMessage: (String) -> Unit
) {
    val msgState = remember { mutableStateOf("") }
    val isKeyboardShow = remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = grayscaleBackground)
    ) {
        IconButton(
            onClick = {
            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),

        ) {
            Icon(
                painterResource(R.drawable.ic_photos),
                contentDescription = "",
                tint = grayscale1,
            )
        }

        IconButton(
            onClick = {

            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_link),
                contentDescription = "",
                tint = grayscale1,
            )
        }

        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            CKTextInputFieldChat(
                "Enter message...",
                msgState,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(painter = painterResource(id = R.drawable.ic_icon), contentDescription = null,
                            tint = MaterialTheme.colors.surface
                        )
                    }
                },)
        }
        IconButton(
            onClick = {
                if (!TextUtils.isEmpty(msgState.value)) {
                    val message = msgState.value
                    onSendMessage(message)
                    // clear text
                    msgState.value = ""
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),
        ) {
            Icon(
                painterResource(
                    id = R.drawable.ic_send_plane
                ),
                contentDescription = "",
                tint = MaterialTheme.colors.surface,
            )
        }
    }
}
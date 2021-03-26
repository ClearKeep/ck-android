package com.clearkeep.screen.chat.room.composes

import android.text.TextUtils
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTextField

@ExperimentalComposeUiApi
@Composable
fun SendBottomCompose(
    onSendMessage: (String) -> Unit
) {
    val msgState = remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(0.66f).padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            CKTextField(
                "Enter message...",
                "",
                msgState,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None
            )
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
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "",
                tint = MaterialTheme.colors.surface,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
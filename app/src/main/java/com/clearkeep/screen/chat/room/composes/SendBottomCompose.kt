package com.clearkeep.screen.chat.room.composes

import android.text.TextUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTextField

@Composable
fun SendBottomCompose(
    onSendMessage: (String) -> Unit
) {
    val msgState = mutableStateOf("")

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            color = Color.White,
            modifier = Modifier.weight(0.66f),
        ) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                CKTextField(
                        "Enter message...",
                        " ",
                        msgState,
                        keyboardType = KeyboardType.Text,
                )
            }
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
                tint = Color.Blue,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
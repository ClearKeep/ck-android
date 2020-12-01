package com.clearkeep.screen.chat.room.composes

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.components.base.CKTextField

@Composable
fun SendBottomCompose(
    onSendMessage: (String) -> Unit
) {
    val msgState = state { "" }

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
                        msgState
                )
            }
        }
        IconButton(
            onClick = {
                if (!TextUtils.isEmpty(msgState.value)) {
                    onSendMessage(msgState.value)
                    // clear text
                    msgState.value = ""
                }
            },
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(Icons.Filled.Send.copy(defaultHeight = 36.dp, defaultWidth = 36.dp), tint = Color.Blue)
        }
    }
}
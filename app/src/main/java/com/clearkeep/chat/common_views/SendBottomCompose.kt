package com.clearkeep.chat.common_views

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.ui.base.CKTextField

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
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                CKTextField(
                        "Next Message",
                        " ",
                        msgState
                )
            }
        }
        Button(
            modifier = Modifier.padding(8.dp),
            onClick = {
                if (!TextUtils.isEmpty(msgState.value)) {
                    onSendMessage(msgState.value)
                    // clear text
                    msgState.value = ""
                }
            }
        ) {
            Text(
                text = "Send",
                style = TextStyle(fontSize = TextUnit.Sp(16))
            )
        }
    }
}

@Preview
@Composable
fun PreviewApp() {
    SendBottomCompose(
        onSendMessage = {}
    )

}
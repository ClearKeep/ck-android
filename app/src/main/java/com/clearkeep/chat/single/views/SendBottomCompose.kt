package com.clearkeep.chat.single.views

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.ui.HintEditText

@Composable
fun SendBottomCompose(
    onSendMessage: (String) -> Unit
) {
    val msgState = state { TextFieldValue("") }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
                    + Modifier.weight(0.66f),
            shape = RoundedCornerShape(4.dp)
        ) {
            HintEditText(
                hintText = "Next Message",
                modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth(),
                textValue = msgState
            )
        }
        Button(
            modifier = Modifier.padding(8.dp),
            onClick = {
                if (!TextUtils.isEmpty(msgState.value.text)) {
                    onSendMessage(msgState.value.text)
                    // clear text
                    msgState.value = TextFieldValue("")
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
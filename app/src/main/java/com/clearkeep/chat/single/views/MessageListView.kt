package com.clearkeep.chat.single.views

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.db.model.Message

@Composable
fun MessageListView(
    state: MutableState<List<Message>>
) {
    ScrollableColumn {
        state.value.forEach {
            Column {
                Row {
                    Text(text = it.message)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Preview
@Composable
fun MessageListPreview() {
    MessageListView(
        state = mutableStateOf(listOf(Message("dai", "hello"), Message("dai", "hello")))
    )
}
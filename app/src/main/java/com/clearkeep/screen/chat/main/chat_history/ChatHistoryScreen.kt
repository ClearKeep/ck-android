package com.clearkeep.screen.chat.main.chat_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.utilities.getHourTimeAsString

@Composable
fun ChatHistoryScreen(
        chatViewModel: ChatViewModel,
        onRoomSelected: (ChatGroup) -> Unit,
        onCreateGroup: () -> Unit
) {
    val rooms = chatViewModel.groups.observeAsState()
    Column {
        CKTopAppBar(
                title = {
                    Text(text = "Chat")
                },
                actions = {
                    CKTextButton(
                        title = stringResource(R.string.btn_create_group),
                        onClick = onCreateGroup,
                    )
                }
        )
        rooms?.value?.let { values ->
            if (values.isEmpty()) {
                EmptySuggestion()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 20.dp,
                        end = 20.dp,
                        start = 20.dp,
                        bottom = 20.dp
                    ),
                ) {
                    itemsIndexed(values) { _, room ->
                        RoomItem(
                            room,
                            chatViewModel.getClientId(),
                            onRoomSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySuggestion() {
    Column(
        modifier = Modifier.fillMaxSize().padding(50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.empty_conversation_history),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun RoomItem(
        room: ChatGroup,
        ourClientId: String,
        onRoomSelected: (ChatGroup) -> Unit,
) {
    val roomName = if (room.isGroup()) room.groupName else {
        room.clientList.firstOrNull { client ->
            client.id != ourClientId
        }?.userName ?: ""
    }
    Column(modifier = Modifier
            .clickable(onClick = { onRoomSelected(room) }, enabled = true)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(emptyList(), roomName, isGroup = room.isGroup())
            Column(modifier = Modifier
                .padding(start = 20.dp)
                .fillMaxWidth()) {
                Row {
                    Text(text = roomName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1.0f, true)
                    )
                    room.lastMessage?.let { lastMessage ->
                        Text(text = getHourTimeAsString(lastMessage.createdTime),
                            style = MaterialTheme.typography.caption.copy(fontSize = 8.sp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        )
                    }
                }
                room.lastMessage?.let { lastMessage ->
                    Text(text = lastMessage.message,
                        style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}
package com.clearkeep.screen.chat.room

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.screen.chat.room.composes.MessageListView
import com.clearkeep.screen.chat.room.composes.SendBottomCompose
import java.lang.IllegalArgumentException
import androidx.navigation.compose.*

@Composable
fun RoomScreen(
        groupId: String?,
        friendId: String?,

        roomViewModel: RoomViewModel,
        navHostController: NavHostController,
        onFinishActivity: () -> Unit,
) {
    if (TextUtils.isEmpty(groupId) && TextUtils.isEmpty(friendId)) {
        throw IllegalArgumentException("Can not load room with both groupId and friendId is empty")
    }
    if (!groupId.isNullOrEmpty()) {
        roomViewModel.updateGroupWithId(groupId)
    } else if (!friendId.isNullOrEmpty()) {
        roomViewModel.updateGroupWithFriendId(friendId)
    }

    RoomObserverView(
            roomViewModel = roomViewModel,
            navHostController = navHostController,
            onFinishActivity = onFinishActivity
    )
}

@Composable
fun RoomObserverView(
        roomViewModel: RoomViewModel,
        navHostController: NavHostController,
        onFinishActivity: () -> Unit,
) {
    val group = roomViewModel.group.observeAsState()
    group?.value?.let { group ->
        val messageList = roomViewModel.getMessages(group.id).observeAsState()
        val groupName = if (group.isGroup()) group.groupName else {
            group.clientList.first { client ->
                client.id != roomViewModel.getClientId()
            }.userName
        }
        Column(
                modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                    title = {
                        Box(modifier = Modifier.clickable(onClick = {
                            navHostController.navigate("room_info_screen")
                        })) {
                            Text(text = groupName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                                onClick = {
                                    onFinishActivity()
                                }
                        ) {
                            Icon(asset = Icons.Filled.ArrowBack)
                        }
                    },
            )
            Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
                    0.66f
            )) {
                messageList?.value?.let { messages ->
                    MessageListView(
                        messageList = messages,
                        clients = group.clientList,
                        myClientId = roomViewModel.getClientId(),
                        group.isGroup()
                    )
                }
            }
            SendBottomCompose(
                    onSendMessage = { message ->
                        val groupResult = group
                        val isGroup = groupResult.isGroup()
                        if (isGroup) {
                            roomViewModel.sendMessageToGroup(groupResult.id, message, groupResult.isJoined)
                        } else {
                            val friend = groupResult.clientList.first { client ->
                                client.id != roomViewModel.getClientId()
                            }
                            roomViewModel.sendMessageToUser(friend, groupResult.id, message)
                        }
                    }
            )
        }
    }
}

package com.clearkeep.chat.group.compose

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.chat.common_views.MessageListView
import com.clearkeep.chat.common_views.SendBottomCompose
import com.clearkeep.chat.group.GroupChatViewModel
import com.clearkeep.ui.base.CKButton

@Composable
fun GroupRoomScreen(
        navController: NavHostController,
        roomId: Int,
        roomName: String,
        groupId: String,
        groupChatViewModel: GroupChatViewModel
) {
    val messageList = groupChatViewModel.getMessagesInRoom(roomId).observeAsState()
    val room = groupChatViewModel.getRoom(roomId).observeAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
                title = {
                    Text(text = roomName)
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                navController.popBackStack(navController.graph.startDestination, false)
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        room.let {
            val isAccepted = room?.value?.isAccepted ?: true
            if (!isAccepted) {
                Row (
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CKButton(
                        "Join group",
                        onClick = {
                            groupChatViewModel.joinInGroup(roomId, groupId)
                        }
                    )
                }
            } else {
                Column {
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
            0.66f
        )) {
            messageList?.let {
                MessageListView(
                        messageList = it.value ?: emptyList(),
                        myClientId = groupChatViewModel.getClientId()
                )
            }
        }
        SendBottomCompose(
                onSendMessage = { message -> groupChatViewModel.sendMessage(roomId, groupId, message) }
        )
        Spacer(modifier = Modifier.height(60.dp))
    }
}
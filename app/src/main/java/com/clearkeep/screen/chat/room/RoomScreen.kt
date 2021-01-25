package com.clearkeep.screen.chat.room

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.screen.chat.room.composes.MessageListView
import com.clearkeep.screen.chat.room.composes.SendBottomCompose
import androidx.navigation.compose.*
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK

@Composable
fun RoomScreen(
        roomViewModel: RoomViewModel,
        navHostController: NavHostController,
        onFinishActivity: () -> Unit,
) {
    val group = roomViewModel.group.observeAsState()
    group?.value?.let { group ->
        val messageList = roomViewModel.getMessages(group.id).observeAsState()
        val groupName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.id != roomViewModel.getClientId()
            }?.userName ?: ""
        }
        val requestCallViewState = roomViewModel.requestCallState.observeAsState()
        Stack(
                modifier = Modifier.fillMaxSize(),
        ) {
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
                        actions = {
                            IconButton(
                                    /*enabled = requestCallViewState.value?.status != Status.LOADING,*/
                                    onClick = {
                                        roomViewModel.requestCall(group.id)
                                    }
                            ) {
                                Icon(asset = Icons.Filled.VideoCall)
                            }
                        }
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
                                val friend = groupResult.clientList.firstOrNull { client ->
                                    client.id != roomViewModel.getClientId()
                                }
                                if (friend != null) {
                                    roomViewModel.sendMessageToUser(friend, groupResult.id, message)
                                } else {
                                    printlnCK("can not found friend")
                                }
                            }
                        }
                )
            }
            requestCallViewState?.value?.let {
                printlnCK("status = ${it.status}")
                if (Status.LOADING == it.status) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = Color.Blue)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "creating group...", style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

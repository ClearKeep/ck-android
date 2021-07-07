package com.clearkeep.screen.chat.room

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.screen.chat.room.composes.MessageListView
import com.clearkeep.screen.chat.room.composes.SendBottomCompose
import androidx.navigation.compose.*
import com.clearkeep.screen.chat.room.composes.ToolbarMessage
import com.clearkeep.components.base.TopBoxCallingStatus
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun RoomScreen(
        roomViewModel: RoomViewModel,
        navHostController: NavHostController,
        onFinishActivity: () -> Unit,
        onCallingClick: ((isPeer: Boolean) -> Unit)
) {
    val group = roomViewModel.group.observeAsState()
    group.value?.let { group ->
        if (group.groupId != GROUP_ID_TEMPO) {
            roomViewModel.setJoiningRoomId(group.groupId)
        }
        val messageList = roomViewModel.getMessages(group.groupId).observeAsState()
        val groupName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.id != roomViewModel.getClientId()
            }?.userName ?: ""
        }
        val requestCallViewState = roomViewModel.requestCallState.observeAsState()
        Box(
                modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                    modifier = Modifier.fillMaxSize()
            ) {
                val remember = AppCall.listenerCallingState.observeAsState()
                remember.value?.let {
                    if (it.isCalling)
                        TopBoxCallingStatus(callingStateData = it, onClick = { isCallPeer -> onCallingClick(isCallPeer)})
                }
                ToolbarMessage(modifier = Modifier, groupName,isGroup = group.isGroup(), onBackClick = {
                    onFinishActivity()
                }, onUserClick = {
                    if (group.isGroup()) {
                        navHostController.navigate("room_info_screen")
                    }
                }, onAudioClick = {
                    roomViewModel.requestCall(group.groupId, true)

                },onVideoClick = {
                    roomViewModel.requestCall(group.groupId, false)

                })
                Column(modifier = Modifier
                    .weight(
                        0.66f
                    )) {
                    messageList?.value?.let { messages ->
                        MessageListView(
                                messageList = messages,
                                clients = group.clientList,
                                myClientId = roomViewModel.getClientId(),
                                group.isGroup(),
                        )
                    }
                }
                SendBottomCompose(
                        onSendMessage = { message ->
                            val validMessage = message.trim().dropLastWhile { it.equals("\\n") || it.equals("\\r") }
                            if (validMessage .isEmpty()) {
                                return@SendBottomCompose
                            }
                            val groupResult = group
                            val isGroup = groupResult.isGroup()
                            if (isGroup) {
                                roomViewModel.sendMessageToGroup(groupResult.groupId, validMessage, groupResult.isJoined)
                            } else {
                                val friend = groupResult.clientList.firstOrNull { client ->
                                    client.id != roomViewModel.getClientId()
                                }
                                if (friend != null) {
                                    roomViewModel.sendMessageToUser(friend, groupResult.groupId, validMessage)
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

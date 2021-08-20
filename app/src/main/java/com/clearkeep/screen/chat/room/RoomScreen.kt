package com.clearkeep.screen.chat.room

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.clearkeep.components.*
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.screen.chat.room.composes.MessageListView
import com.clearkeep.screen.chat.room.composes.SendBottomCompose
import com.clearkeep.screen.chat.room.composes.ToolbarMessage
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.core.os.postDelayed
import com.clearkeep.R
import com.clearkeep.screen.chat.room.file_picker.FilePickerBottomSheetDialog
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun RoomScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
    onFinishActivity: () -> Unit,
    onCallingClick: ((isPeer: Boolean) -> Unit),
) {
    val systemUiController = rememberSystemUiController()
    val group = roomViewModel.group.observeAsState()
    val isNote = roomViewModel.isNote.observeAsState()
    val isUploadPhotoDialogVisible = remember { mutableStateOf(false) }
    val uploadFileResponse = roomViewModel.uploadFileResponse.observeAsState()
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val isShowDialogCalling = remember { mutableStateOf(false) }
    val listPeerAvatars = roomViewModel.listPeerAvatars.observeAsState()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = true
        )
    }

    if (group.value != null || isNote.value == true) {
        val group = group.value
        if (group != null && group.groupId != GROUP_ID_TEMPO) {
            roomViewModel.setJoiningRoomId(group.groupId)
        }
        val messageList = if (group != null) {
            roomViewModel.getMessages(group.groupId, group.ownerDomain, group.ownerClientId)
                .observeAsState()
        } else {
            roomViewModel.getNotes().observeAsState()
        }

        printlnCK("test: ${group?.clientList}")
        val groupName = group?.groupName ?: "Note"
        val requestCallViewState = roomViewModel.requestCallState.observeAsState()
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetContent = {
                FilePickerBottomSheetDialog(roomViewModel) {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        if (isNote.value == true) {
                            roomViewModel.uploadFile(context, group?.groupId ?: 0L, null, null)
                        } else if (group != null) {
                            val isGroup = group.isGroup()
                            if (isGroup) {
                                roomViewModel.uploadFile(context, group.groupId, group.isJoined)
                            } else {
                                val friend = group.clientList.firstOrNull { client ->
                                    client.userId != roomViewModel.clientId
                                }
                                if (friend != null) {
                                    roomViewModel.uploadFile(context, group.groupId, null, friend)
                                } else {
                                    printlnCK("can not found friend")
                                }
                            }
                        }
                    }
                }
            },
            sheetBackgroundColor = bottomSheetColor,
            scrimColor = colorDialogScrim
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ToolbarMessage(
                        modifier = Modifier,
                        groupName,
                        listPeerAvatars.value,
                        isGroup = group?.isGroup() ?: false,
                        isNote = isNote.value ?: false,
                        onBackClick = {
                            onFinishActivity()
                        },
                        onUserClick = {
                            if (group?.isGroup() == true) {
                                navHostController.navigate("room_info_screen")
                            }
                        },
                        onAudioClick = {
                            if (AppCall.listenerCallingState.value?.isCalling != true) {
                                roomViewModel.requestCall(group?.groupId ?: 0L, true)
                            }else{
                                isShowDialogCalling.value = true
                            }
                        },
                        onVideoClick = {
                            if (AppCall.listenerCallingState.value?.isCalling != true) {
                                roomViewModel.requestCall(group?.groupId ?: 0L, false)
                            }else{
                                isShowDialogCalling.value = true
                            }
                        })
                    Column(
                        modifier = Modifier
                            .weight(
                                0.66f
                            )
                    ) {
                        messageList.value?.let { messages ->
                            MessageListView(
                                messageList = messages,
                                clients = group?.clientList ?: emptyList(),
                                myClientId = roomViewModel.clientId,
                                group?.isGroup() ?: false,
                                onClickFile = {
                                    roomViewModel.downloadFile(context, it)
                                },
                                onClickImage = { uris: List<String>, senderName: String ->
                                    roomViewModel.setImageDetailList(uris)
                                    roomViewModel.setImageDetailSenderName(senderName)
                                    navHostController.navigate("photo_detail")
                                }
                            )
                        }
                    }
                    SendBottomCompose(
                        roomViewModel,
                        navHostController,
                        onSendMessage = { message ->
                            val validMessage =
                                message.trim()
                                    .dropLastWhile { it.equals("\\n") || it.equals("\\r") }
                            if (validMessage.isEmpty() && roomViewModel.imageUriSelected.value.isNullOrEmpty()) {
                                return@SendBottomCompose
                            }
                            val groupResult = group
                            val isGroup = groupResult?.isGroup()
                            if (isNote.value == true) {
                                roomViewModel.sendNote(context)
                            } else if (isGroup == true) {
                                roomViewModel.sendMessageToGroup(
                                    context,
                                    groupResult.groupId,
                                    validMessage,
                                    groupResult.isJoined
                                )
                            } else {
                                val friend = groupResult?.clientList?.firstOrNull { client ->
                                    client.userId != roomViewModel.clientId
                                }
                                if (friend != null) {
                                    roomViewModel.sendMessageToUser(
                                        context,
                                        friend,
                                        groupResult.groupId,
                                        validMessage
                                    )
                                } else {
                                    printlnCK("can not found friend")
                                }
                            }
                        },
                        onClickUploadPhoto = {
                            focusManager.clearFocus()
                            Handler(Looper.getMainLooper()).postDelayed(
                                KEYBOARD_HIDE_DELAY_MILLIS
                            ) {
                                isUploadPhotoDialogVisible.value = true
                            }
                        },
                        onClickUploadFile = {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                delay(KEYBOARD_HIDE_DELAY_MILLIS)
                                bottomSheetState.show()
                            }
                        }
                    )
                }
            }
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
                        Text(
                            text = "",
                            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        UploadPhotoDialog(isUploadPhotoDialogVisible.value, getPhotoUri = {
            roomViewModel.getPhotoUri(context)
        }, onDismiss = {
            isUploadPhotoDialogVisible.value = false
        }, onNavigateToAlbums = {
            isUploadPhotoDialogVisible.value = false
            navHostController.navigate("image_picker")
        }, onTakePhoto = {
            roomViewModel.addImage()
        })
        val response = uploadFileResponse.value
        if (response?.status == Status.ERROR) {
            CKAlertDialog(
                onDismissButtonClick = {
                    roomViewModel.uploadFileResponse.value = null
                },
                title = response.message ?: "",
            )
        }
        if (isShowDialogCalling.value) {
            CKAlertDialog(
                title = stringResource(R.string.warning),
                text = stringResource(R.string.profile_new_call_warning),
                onDismissButtonClick = {
                    isShowDialogCalling.value = false
                }
            )
        }

    }
}

private const val KEYBOARD_HIDE_DELAY_MILLIS = 500L
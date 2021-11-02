package com.clearkeep.screen.chat.room

import android.Manifest
import android.content.Context
import android.os.*
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
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.os.postDelayed
import com.clearkeep.R
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.chat.room.file_picker.FilePickerBottomSheetDialog
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.ERROR_CODE_TIMEOUT
import com.clearkeep.utilities.isWriteFilePermissionGranted
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.sdp
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
    val groupState = roomViewModel.group.observeAsState()
    val isNote = roomViewModel.isNote.observeAsState()
    val isUploadPhotoDialogVisible = remember { mutableStateOf(false) }
    val isMessageClickDialogVisible = remember { mutableStateOf(false) }
    val uploadFileResponse = roomViewModel.uploadFileResponse.observeAsState()
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val isShowDialogCalling = remember { mutableStateOf(false) }
    val listPeerAvatars = roomViewModel.listPeerAvatars.observeAsState()
    val selectedFileUri = remember { mutableStateOf("") }
    val getGroupResponse = roomViewModel.getGroupResponse.observeAsState()
    val createGroupResponse = roomViewModel.createGroupResponse.observeAsState()
    val inviteToGroupResponse = roomViewModel.inviteToGroupResponse.observeAsState()
    val sendMessageResponse = roomViewModel.sendMessageResponse.observeAsState()

    val requestWriteFilePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                roomViewModel.downloadFile(context, selectedFileUri.value)
            }
        }

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = true
        )
    }

    val group = groupState.value

    if (group != null && group.groupId != GROUP_ID_TEMPO) {
        roomViewModel.setJoiningRoomId(group.groupId)
    }

    val messageList = when {
        group != null -> {
            roomViewModel.getMessages(group.groupId, group.ownerDomain, group.ownerClientId)
                .observeAsState()
        }
        isNote.value == true -> {
            roomViewModel.getNotes().observeAsState()
        }
        else -> null
    }

    val groupName =
        if (isNote.value == true) stringResource(R.string.note) else if (group?.isDeletedUserPeer == true) stringResource(
            R.string.deleted_user
        ) else group?.groupName ?: ""
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
                                roomViewModel.sendMessageResponse.value =
                                    Resource.error("", null, ERROR_CODE_TIMEOUT)
                            }
                        }
                    } else {
                        roomViewModel.sendMessageResponse.value =
                            Resource.error("", null, ERROR_CODE_TIMEOUT)
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
                    title = groupName,
                    avatars = listPeerAvatars.value,
                    isGroup = group?.isGroup() ?: false,
                    isNote = isNote.value ?: false,
                    isDeletedUserChat = group?.isDeletedUserPeer ?: false,
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
                        } else {
                            isShowDialogCalling.value = true
                        }
                    },
                    onVideoClick = {
                        if (AppCall.listenerCallingState.value?.isCalling != true) {
                            roomViewModel.requestCall(group?.groupId ?: 0L, false)
                        } else {
                            isShowDialogCalling.value = true
                        }
                    })
                if (messageList?.value != null) {
                    Column(
                        modifier = Modifier
                            .weight(
                                0.66f
                            )
                    ) {
                        val listUserStatusState = roomViewModel.listUserStatus.observeAsState()
                        MessageListView(
                            messageList = messageList.value!!,
                            clients = group?.clientList ?: emptyList(),
                            listAvatar = listUserStatusState.value ?: emptyList(),
                            myClientId = roomViewModel.clientId,
                            group?.isGroup() ?: false,
                            isLoading = false,
                            onScrollChange = { index, lastTimestamp ->
                              roomViewModel.onScrollChange(lastTimestamp)
                            },
                            onClickFile = {
                                if (isWriteFilePermissionGranted(context)) {
                                    roomViewModel.downloadFile(context, it)
                                } else {
                                    selectedFileUri.value = it
                                    requestWriteFilePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            },
                            onClickImage = { uris: List<String>, senderName: String ->
                                roomViewModel.setImageDetailList(uris)
                                roomViewModel.setImageDetailSenderName(senderName)
                                navHostController.navigate("photo_detail")
                            },
                            onLongClick = {
                                val vibrator =
                                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(
                                            100,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    vibrator.vibrate(100)
                                }
                                roomViewModel.setSelectedMessage(it)
                                isMessageClickDialogVisible.value = true
                            }
                        )
                    }
                } else {
                    Column(Modifier.weight(0.66f)) {
                        Surface(Modifier.fillMaxSize(), color = grayscaleBackground) {
                            Box(Modifier.fillMaxSize()) {
                                //CKCircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
                if (group != null && !group.isDeletedUserPeer) {
                    SendBottomCompose(
                        roomViewModel,
                        onSendMessage = { message ->
                            val validMessage =
                                message.trim()
                                    .dropLastWhile { it.equals("\\n") || it.equals("\\r") }
                            if (validMessage.isEmpty() && roomViewModel.imageUriSelected.value.isNullOrEmpty()) {
                                return@SendBottomCompose
                            }
                            val isGroup = group.isGroup()
                            if (isNote.value == true) {
                                roomViewModel.sendNote(context, )
                            } else if (isGroup) {
                                val isJoined = group.isJoined
                                roomViewModel.sendMessageToGroup(
                                    context,
                                    group.groupId,
                                    validMessage,
                                    isJoined
                                )
                            } else {
                                val friend = group.clientList.firstOrNull { client ->
                                    client.userId != roomViewModel.clientId
                                }
                                if (friend != null) {
                                    roomViewModel.sendMessageToUser(
                                        context,
                                        friend,
                                        group.groupId,
                                        validMessage
                                    )
                                } else {
                                    roomViewModel.sendMessageResponse.value =
                                        Resource.error("", null, ERROR_CODE_TIMEOUT)
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
                        }
                    ) {
                        keyboardController?.hide()
                        coroutineScope.launch {
                            delay(KEYBOARD_HIDE_DELAY_MILLIS)
                            bottomSheetState.show()
                        }
                    }
                }
            }
            val isLoading = roomViewModel.isLoading.observeAsState()
            if (isLoading.value == true)
                CKCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

    }
    requestCallViewState.value?.let {
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
                    Spacer(modifier = Modifier.height(10.sdp()))
                    Text(
                        text = "",
                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else if (it.status == Status.ERROR) {
            CKAlertDialog(
                title = stringResource(R.string.network_error_dialog_title),
                text = stringResource(R.string.network_error_dialog_text),
                onDismissButtonClick = {
                    roomViewModel.requestCallState.value = null
                }
            )
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
    MessageClickDialog(roomViewModel, isMessageClickDialogVisible.value, onDismiss = {
        isMessageClickDialogVisible.value = false
    })
    val response = uploadFileResponse.value
    if (response?.status == Status.ERROR) {
        CKAlertDialog(
            onDismissButtonClick = {
                roomViewModel.uploadFileResponse.value = null
            },
            title = stringResource(R.string.message_sent_error),
            text = response.message ?: "",
            dismissTitle = stringResource(R.string.close)
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
    if ((getGroupResponse.value?.status == Status.ERROR && getGroupResponse.value?.errorCode == ERROR_CODE_TIMEOUT)
        || (createGroupResponse.value?.status == Status.ERROR && createGroupResponse.value?.errorCode == ERROR_CODE_TIMEOUT)
        || (inviteToGroupResponse.value?.status == Status.ERROR && inviteToGroupResponse.value?.errorCode == ERROR_CODE_TIMEOUT)
        || (sendMessageResponse.value?.status == Status.ERROR && sendMessageResponse.value?.errorCode == ERROR_CODE_TIMEOUT)
    ) {
        CKAlertDialog(
            title = stringResource(R.string.network_error_dialog_title),
            text = stringResource(R.string.network_error_dialog_text),
            onDismissButtonClick = {
                roomViewModel.getGroupResponse.value = null
                roomViewModel.createGroupResponse.value = null
                roomViewModel.inviteToGroupResponse.value = null
                roomViewModel.sendMessageResponse.value = null
            }
        )
    }
}

@ExperimentalComposeUiApi
@Composable
fun MessageClickDialog(
    roomViewModel: RoomViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    if (isOpen) {
        Box {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorDialogScrim)
                    .clickable {
                        onDismiss()
                    })
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    Text(
                        stringResource(R.string.copy),
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                roomViewModel.copySelectedMessage(context)
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.copied),
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                onDismiss()
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box {
                    Text(
                        stringResource(R.string.cancel), modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private const val KEYBOARD_HIDE_DELAY_MILLIS = 500L
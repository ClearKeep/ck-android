package com.clearkeep.screen.chat.room

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.clearkeep.components.*
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.TopBoxCallingStatus
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.screen.chat.room.composes.MessageListView
import com.clearkeep.screen.chat.room.composes.SendBottomCompose
import com.clearkeep.screen.chat.room.composes.ToolbarMessage
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.FileProvider
import androidx.core.os.postDelayed
import com.clearkeep.BuildConfig
import com.clearkeep.screen.chat.room.file_picker.FilePickerBottomSheetDialog
import kotlinx.coroutines.launch
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
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
    val group = roomViewModel.group.observeAsState()
    val isUploadPhotoDialogVisible = remember { mutableStateOf(false) }
    val uploadFileResponse = roomViewModel.uploadFileResponse.observeAsState()
    val context = LocalContext.current
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    group.value?.let { group ->
        if (group.groupId != GROUP_ID_TEMPO) {
            roomViewModel.setJoiningRoomId(group.groupId)
        }
        val messageList = roomViewModel.getMessages(group.groupId, group.ownerDomain, group.ownerClientId).observeAsState()
        printlnCK("test: ${group.clientList}")
        val groupName = group.groupName
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
                    ToolbarMessage(modifier = Modifier, groupName, isGroup = group.isGroup(), onBackClick = {
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
                    BottomSheetScaffold(
                        scaffoldState = bottomSheetScaffoldState,
                        sheetContent = {
                            FilePickerBottomSheetDialog(roomViewModel, onClickNext = {
                                coroutineScope.launch {
                                    bottomSheetScaffoldState.bottomSheetState.collapse()
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
                            })
                        },
                        sheetPeekHeight = 0.dp,
                        sheetBackgroundColor = Color(0xF3FFFFFF)
                    ) {
                        Column(modifier = Modifier
                            .weight(
                                0.66f
                            )) {
                            messageList?.value?.let { messages ->
                                MessageListView(
                                    messageList = messages,
                                    clients = group.clientList,
                                    myClientId = roomViewModel.clientId,
                                    group.isGroup(),
                                ) {
                                    roomViewModel.downloadFile(context, it)
                                }
                            }
                        }
                        SendBottomCompose(
                            roomViewModel,
                            navHostController,
                            onSendMessage = { message ->
                                val validMessage = message.trim().dropLastWhile { it.equals("\\n") || it.equals("\\r") }
                                if (validMessage.isEmpty() && roomViewModel.imageUriSelected.value.isNullOrEmpty()) {
                                    return@SendBottomCompose
                                }
                                val groupResult = group
                                val isGroup = groupResult.isGroup()
                                if (isGroup) {
                                    roomViewModel.sendMessageToGroup(context, groupResult.groupId, validMessage, groupResult.isJoined)
                                } else {
                                    val friend = groupResult.clientList.firstOrNull { client ->
                                        client.userId != roomViewModel.clientId
                                    }
                                    if (friend != null) {
                                        roomViewModel.sendMessageToUser(context, friend, groupResult.groupId, validMessage)
                                    } else {
                                        printlnCK("can not found friend")
                                    }
                                }
                            },
                            onClickUploadPhoto = {
                                focusManager.clearFocus()
                                Handler(Looper.getMainLooper()).postDelayed(
                                    KEYBOARD_HIDE_DELAY_MILLIS) {
                                    isUploadPhotoDialogVisible.value = true
                                }
                            },
                            onClickUploadFile = {
                                keyboardController?.hide()
                                coroutineScope.launch {
                                    delay(KEYBOARD_HIDE_DELAY_MILLIS)
                                    bottomSheetScaffoldState.bottomSheetState.expand()
                                }
                            }
                        )
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
                                text = "creating group...",
                                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            UploadPhotoDialog(isUploadPhotoDialogVisible.value, onDismiss = {
                isUploadPhotoDialogVisible.value = false
            }, onNavigateToAlbums = {
                isUploadPhotoDialogVisible.value = false
                navHostController.navigate("image_picker")
            }, onTakePhoto = {
                roomViewModel.addImage(it)
            })
            val response = uploadFileResponse.value
            if (response?.status == Status.ERROR) {
                CKAlertDialog(onDismissButtonClick = {
                    roomViewModel.uploadFileResponse.value = null
                }, title =response.message ?: "",
                )
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun UploadPhotoDialog(isOpen: Boolean, onDismiss: () -> Unit, onNavigateToAlbums: () -> Unit, onTakePhoto: (String) -> Unit) {
    val context = LocalContext.current

    val requestStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onNavigateToAlbums()
        } else {
            onDismiss()
        }
    }

    val uri = generatePhotoUri(context)
    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSuccessful : Boolean ->
        if (isSuccessful) {
            onTakePhoto(uri.toString())
            onDismiss()
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            takePhotoLauncher.launch(uri)
        } else {
            onDismiss()
        }
    }

    if (isOpen) {
        Box {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
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
                    Text("Take a photo",
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                if (isCameraPermissionGranted(context)) {
                                    takePhotoLauncher.launch(uri)
                                } else {
                                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                    Divider(color = separatorDarkNonOpaque)
                    Text("Albums",
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                if (isFilePermissionGranted(context)) {
                                    onNavigateToAlbums()
                                } else {
                                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }, textAlign = TextAlign.Center, color = tintsRedLight
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box {
                    Text(
                        "Cancel", modifier = Modifier
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

private fun isFilePermissionGranted(context: Context): Boolean {
    return isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun isCameraPermissionGranted(context: Context): Boolean {
    return isPermissionGranted(context, Manifest.permission.CAMERA)
}

private fun isPermissionGranted(context: Context, permission: String): Boolean  {
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, permission) -> {
            true
        }
        else -> {
            false
        }
    }
}

private fun generatePhotoUri(context: Context) : Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(
        context.applicationContext,
        BuildConfig.APPLICATION_ID + ".provider",
        file
    )
}

private const val KEYBOARD_HIDE_DELAY_MILLIS = 500L
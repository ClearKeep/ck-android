package com.clearkeep.screen.chat.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.home.composes.SideBarLabel
import com.clearkeep.screen.chat.room.UploadPhotoDialog
import com.clearkeep.utilities.network.Status

@ExperimentalComposeUiApi
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    onCloseView: () -> Unit,
    onChangePassword: () -> Unit,
    onCopyToClipBoard: () -> Unit,
    onNavigateToOtp: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val env = BuildConfig.FLAVOR
    val profile = profileViewModel.profile.observeAsState()
    val context = LocalContext.current

    BackHandler {
        onCloseView()
    }

    profile?.value?.let { user ->
        val userName = profileViewModel.username.observeAsState()
        val email = profileViewModel.email.observeAsState()
        val phoneNumber = profileViewModel.phoneNumber.observeAsState()
        val otpErrorDialogVisible = remember { mutableStateOf(false) }
        val pickAvatarDialogVisible = remember { mutableStateOf(false) }
        val unsavedChangesDialogVisible = profileViewModel.unsavedChangeDialogVisible.observeAsState()
        val uploadAvatarResponse = profileViewModel.uploadAvatarResponse.observeAsState()
        val updateMfaResponse = profileViewModel.updateMfaSettingResponse.observeAsState()
        val selectedAvatar = profileViewModel.imageUriSelected.observeAsState()
        val userPreference = profileViewModel.userPreference.observeAsState()

        Column(
            Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    profile?.value?.let { user ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HeaderProfile(
                                onClickSave = {
                                    profileViewModel.updateProfileDetail(
                                        context,
                                        userName.value ?: "",
                                        phoneNumber.value ?: ""
                                    )
                                },
                                onCloseView = {
                                    onCloseView()
                                }
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleAvatar(
                                    when {
                                        selectedAvatar.value != null -> {
                                            listOf(selectedAvatar.value!!)
                                        }
                                        user.avatar != null -> {
                                            listOf(user.avatar)
                                        }
                                        else -> {
                                            emptyList()
                                        }
                                    },
                                    user.userName ?: "",
                                    size = 72.dp,
                                    modifier = Modifier.clickable {
                                        pickAvatarDialogVisible.value = true
                                    },
                                    cacheKey = user.updatedAt.toString()
                                )
                                Column(
                                    Modifier.padding(start = 16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SideBarLabel(
                                        text = "Change profile picture",
                                        color = primaryDefault,
                                        fontSize = 14.sp,
                                    )
                                    SideBarLabel(
                                        text = " Maximum fize size 5MB",
                                        color = grayscale3,
                                        modifier = Modifier,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            ItemInformationView("Username", userName.value ?: "") {
                                profileViewModel.setUsername(it)
                            }
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView("Email", email.value ?: "", enable = false) {
                                profileViewModel.setEmail(it)
                            }
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView("Phone Number", phoneNumber.value ?: "", keyboardType = KeyboardType.Phone) {
                                profileViewModel.setPhoneNumber(it)
                            }
                            Spacer(Modifier.height(8.dp))
                            CopyLink(
                                onCopied = {
                                    onCopyToClipBoard()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            ChangePassword(onChangePassword)
                            Spacer(Modifier.height(24.dp))
                            TwoFaceAuthView(userPreference.value?.mfa ?: false) {
                                if (it) {
                                    if (profileViewModel.canEnableMfa()) {
                                        profileViewModel.updateMfaSettings(it)
                                    } else {
                                        otpErrorDialogVisible.value = true
                                    }
                                } else {
                                    profileViewModel.updateMfaSettings(it)
                                }
                            }
                            Spacer(Modifier.height(24.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.dp, bottom = 20.dp),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "version $versionName (${env.toUpperCase()})",
                                    style = MaterialTheme.typography.caption.copy(
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        if (otpErrorDialogVisible.value) {
            CKAlertDialog(
                title = "Type in your phone number",
                text = "You must input your phone number in order to enable this feature.",
                dismissTitle = stringResource(R.string.close),
                onDismissButtonClick = {
                    otpErrorDialogVisible.value = false
                }
            )
        }
        if (uploadAvatarResponse.value != null && uploadAvatarResponse.value?.status == Status.ERROR) {
            CKAlertDialog(
                title = uploadAvatarResponse.value!!.message ?: "",
                onDismissButtonClick = {
                    profileViewModel.uploadAvatarResponse.value = null
                },
            )
        }
        if (unsavedChangesDialogVisible.value == true) {
            CKAlertDialog(
                title = stringResource(R.string.profile_unsaved_change_warning),
                dismissTitle = "Stay on this page",
                confirmTitle = "Leave this page",
                onDismissButtonClick = {
                    profileViewModel.unsavedChangeDialogVisible.value = false
                },
                onConfirmButtonClick = {
                    profileViewModel.unsavedChangeDialogVisible.value = false
                    profileViewModel.undoProfileChanges()
                    onCloseView()
                }
            )
        }
        if (updateMfaResponse.value == true && userPreference.value?.mfa == true) {
            onNavigateToOtp()
            profileViewModel.updateMfaSettingResponse.value = null //Prevent response from being handled again
        }

        UploadPhotoDialog(
            isOpen = pickAvatarDialogVisible.value,
            getPhotoUri = { profileViewModel.getPhotoUri(context) },
            onDismiss = { pickAvatarDialogVisible.value = false },
            onNavigateToAlbums = { navController.navigate("pick_avatar") },
            onTakePhoto = {
                profileViewModel.setTakePhoto()
            }
        )
    }
}

@Composable
fun HeaderProfile(onClickSave: () -> Unit, onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CKTextButton(
                    title = "Save",
                    onClick = {
                        onClickSave.invoke()
                    },
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        CKHeaderText("Profile Settings", headerTextType = HeaderTextType.Medium)
        Spacer(modifier = Modifier.size(16.dp))
    }
}


@ExperimentalComposeUiApi
@Composable
fun ItemInformationView(header: String, textValue: String, enable: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = header, style = MaterialTheme.typography.body1.copy(
                color = grayscale1,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        )
        Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, grayscale5)) {
            TextField(
                value = textValue,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(55.dp),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = if (enable) grayscaleBlack else grayscale3,
                    cursorColor = if (enable) grayscaleBlack else grayscale3,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = grayscale5
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = if (enable) grayscaleBlack else grayscale3,
                    fontWeight = FontWeight.Normal
                ),
                enabled = enable,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {keyboardController?.hide()}
                )
            )
        }
    }
}

@Composable
fun CopyLink(onCopied: () -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        SideBarLabel(
            text = "Copy profile link", modifier = Modifier
                .weight(0.66f), fontSize = 14.sp, color = MaterialTheme.colors.primary
        )
        Column(
            modifier = Modifier.clickable { },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onCopied.invoke() }
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "",
                    tint = primaryDefault
                )
            }
        }
    }
}

@Composable
fun ChangePassword(onChangePassword: () -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        SideBarLabel(
            text = "Change Password", modifier = Modifier
                .weight(0.66f), fontSize = 14.sp, color = MaterialTheme.colors.primary
        )
        Column(
            modifier = Modifier.clickable { },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onChangePassword.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "",
                    tint = primaryDefault
                )
            }
        }
    }
}

@Composable
fun TwoFaceAuthView(
    enabled: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Column {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                "Two Factors Authentication",
                modifier = Modifier.weight(0.66f)
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onCheckChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryDefault, checkedTrackColor = primaryDefault,
                        uncheckedThumbColor = grayscale3, uncheckedTrackColor = grayscale3
                    ),
                    modifier = Modifier
                        .width(64.dp)
                        .height(36.dp)
                )
            }
        }

        Text(
            text = "Give your account more protection", style = MaterialTheme.typography.body1.copy(
                color = grayscale2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        )

        Text(
            text = "scam and account hacking ", style = MaterialTheme.typography.body1.copy(
                color = grayscale2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }

}
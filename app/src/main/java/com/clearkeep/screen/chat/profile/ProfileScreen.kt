package com.clearkeep.screen.chat.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.home.composes.SideBarLabel

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onCloseView: () -> Unit,
    onChangePassword: () -> Unit,
    onCopyToClipBoard: () -> Unit,
    onNavigateToOtp: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val env = BuildConfig.FLAVOR
    val profile = profileViewModel.profile.observeAsState()
    profile?.value?.let { user ->
        val userName = remember { mutableStateOf(user.userName.toString()) }
        val email = remember { mutableStateOf(user.email.toString()) }
        val phoneNumber = remember { mutableStateOf("") }
        val authCheckedChange = remember { mutableStateOf(false) }
        val otpErrorDialogVisible = remember { mutableStateOf(false) }
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
                                    profileViewModel.updateProfileDetail(userName.value, phoneNumber.value)
                            }, onCloseView)
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleAvatar(emptyList(), user.userName ?: "", size = 72.dp)
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
                            ItemInformationView("Username", userName)
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView("Email", email, enable = false)
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView("Phone Number", phoneNumber, keyboardType = KeyboardType.Phone)
                            Spacer(Modifier.height(8.dp))
                            CopyLink(
                                onCopied = {
                                    onCopyToClipBoard()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            ChangePassword(onChangePassword)
                            Spacer(Modifier.height(24.dp))
                            TwoFaceAuthView(authCheckedChange) {
                                if (it) {
                                    if (phoneNumber.value.isNotBlank()) {
                                        authCheckedChange.value = true
                                        onNavigateToOtp()
                                    } else {
                                        otpErrorDialogVisible.value = true
                                    }
                                } else {
                                    authCheckedChange.value = false
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
                onDismissButtonClick = {
                    otpErrorDialogVisible.value = false
                }
            )
        }
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


@Composable
fun ItemInformationView(header: String, textValue: MutableState<String>, enable: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text) {
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
                value = textValue.value,
                onValueChange = { textValue.value = it },
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
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
    mutableState: MutableState<Boolean>,
    onCheckChange: (Boolean) -> Unit
) {
    Column() {
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
                    checked = mutableState.value,
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

@Composable
fun LogoutConfirmDialog(
    showReminder: Boolean,
    setShowDialog: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    if (showReminder) {
        CKAlertDialog(
            title = "Logout",
            text = "Do you want to log out?",
            onDismissButtonClick = {
                setShowDialog(false)
            },
            dismissTitle = "Cancel",
            onConfirmButtonClick = {
                setShowDialog(false)
                onLogout.invoke()
            },
        )
    }
}
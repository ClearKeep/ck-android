package com.clearkeep.screen.chat.home.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.home.MainViewModel
import com.clearkeep.screen.chat.composes.CircleAvatar

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    homeViewModel: MainViewModel,
    onCloseView: () -> Unit,
    onChangePassword: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val env = BuildConfig.FLAVOR
    val profile = profileViewModel.profile.observeAsState()
    val userName = remember { mutableStateOf(profile.value?.userName.toString()) }
    val email = remember { mutableStateOf(profile.value?.email.toString()) }
    val phoneNumber = remember { mutableStateOf("") }
    val authCheckedChange = remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isSystemInDarkTheme()) grayscale1 else backgroundGradientStart,
                        if (isSystemInDarkTheme()) grayscale5 else backgroundGradientEnd
                    )
                )
            )
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White
                    ), horizontalAlignment = Alignment.CenterHorizontally

            ) {
                profile?.value?.let { user ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HeaderProfile(onCloseView)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleAvatar(emptyList(), user.userName ?: "", size = 72.dp)
                            Column(
                                Modifier.padding(start = 16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                CkTextNormal(
                                    text = "Change profile picture",
                                    color = primaryDefault,
                                    fontSize = 14.sp,
                                )
                                CkTextNormal(
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
                        ItemInformationView("Phone Number", phoneNumber)
                        Spacer(Modifier.height(8.dp))
                        ChangePassword(onChangePassword)
                        Spacer(Modifier.height(24.dp))
                        TwoFaceAuthView (authCheckedChange)
                    }
                }
                Spacer(Modifier.height(40.dp))

            }
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

@Composable
fun HeaderProfile(onCloseView: () -> Unit) {
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
                    },
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        CkTextNormal(text = "Profile Settings", fontSize = 20.sp, color = grayscaleBlack)
        Spacer(modifier = Modifier.size(16.dp))
    }
}


@Composable
fun ItemInformationView(header: String, textValue: MutableState<String>, enable: Boolean = true) {
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
                ), enabled = enable
            )
        }
    }
}

@Composable
fun ChangePassword(onChangePassword: () -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        CkTextNormal(
            text = "Change Password", modifier = Modifier
                .weight(0.66f), fontSize = 14.sp, color = primaryDefault
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
fun TwoFaceAuthView(mutableState: MutableState<Boolean>) {
    Column() {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CkTextNormal(
                text = "Two Factors Authentication", modifier = Modifier
                    .weight(0.66f), fontSize = 16.sp, color = grayscaleBlack
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Switch(
                    checked = mutableState.value,
                    onCheckedChange = { mutableState.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = primaryDefault,checkedTrackColor = primaryDefault,
                    uncheckedThumbColor = grayscale3,uncheckedTrackColor = grayscale3),
                    modifier = Modifier.width(64.dp).height(36.dp)
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
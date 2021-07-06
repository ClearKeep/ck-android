package com.clearkeep.screen.chat.main.home.composes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.screen.chat.main.home.HomeViewModel
import com.clearkeep.screen.chat.main.profile.LogoutConfirmDialog
import com.clearkeep.screen.chat.main.profile.ProfileViewModel

@Composable
fun SiteMenuScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavController,
    closeSiteMenu: (() -> Unit),
    onLogout: (() -> Unit)
) {
    val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(grayscaleOverlay)
            .focusable()
            .clickable(enabled = true, onClick = { null })
    ) {
        Row(
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        //todo disable dark mode
                        if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                        if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                    )
                ),
                alpha = 0.4f
            )
        ) {
            Box(
                Modifier
                    .width(108.dp)
            ) {
            }
            Card(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, bottom = 20.dp),
                backgroundColor = Color.White,
                shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp),
                elevation = 8.dp
            ) {
                Box {
                    Column {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    onClick = { closeSiteMenu.invoke() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "",
                                        tint = MaterialTheme.colors.primaryVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                            HeaderSite(profileViewModel)
                            Spacer(modifier = Modifier.size(24.dp))
                            Divider(color = grayscale3)
                            SettingGeneral(navController) {
                                setShowReminderDialog.invoke(true)
                            }
                            Divider(color = grayscale3)
                            SettingServer(
                                "CK Development", navController
                            )
                        }
                    }

                    Row(modifier = Modifier
                        .padding(top = 38.dp, bottom = 38.dp)
                        .align(Alignment.BottomCenter)
                        .clickable { },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = null,
                            tint = errorDefault
                        )
                        Text(
                            text = "Logout", modifier = Modifier
                                .padding(start = 16.dp), style = TextStyle(
                                color = errorDefault ?: MaterialTheme.colors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        LogoutConfirmDialog(showReminder, setShowReminderDialog, onLogout)
    }
}

@Composable
fun HeaderSite(profileViewModel: ProfileViewModel) {
    val profile = profileViewModel.profile.observeAsState()
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarSite(url = "", name = profile.value?.userName ?: "", status = "")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CKHeaderText(
                text = profile.value?.userName ?: "",
                headerTextType = HeaderTextType.Normal,
                color = primaryDefault
            )
            Row(modifier = Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Online",
                    style = TextStyle(color = colorSuccessDefault, fontSize = 14.sp)
                )
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chev_down),
                        null,
                        alignment = Alignment.Center
                    )
                }
            }
            StatusDropdown(expanded, onDismiss = { expanded = false })
        }
    }
}

@Composable
fun SettingServer(
    serverName: String, navController: NavController,
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        CKHeaderText(
            text = "Server Setting",
            headerTextType = HeaderTextType.Normal,
            color = grayscale2
        )
        ItemSiteSetting("Server", R.drawable.ic_adjustment, {
            navController.navigate("server_setting")
        })
        ItemSiteSetting("Notification", R.drawable.ic_server_notification)
        ItemSiteSetting("Invite", R.drawable.ic_user_plus)
        ItemSiteSetting("Banned", R.drawable.ic_user_off)
        ItemSiteSetting("Leave $serverName", R.drawable.ic_logout)
    }
}

@Composable
fun SettingGeneral(
    navController: NavController,
    onClickAction: () -> Unit
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        ItemSiteSetting("Profile", R.drawable.ic_user, {
            navController.navigate("profile")
        })
    }
}

@Composable
fun ItemSiteSetting(
    name: String,
    icon: Int,
    onClickAction: (() -> Unit)? = null,
    textColor: Color? = null
) {
    Row(modifier = Modifier
        .padding(top = 16.dp, bottom = 18.dp)
        .clickable { onClickAction?.invoke() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null)
        SideBarLabel(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.dp)
        )
    }
}

@Composable
fun StatusDropdown(expanded: Boolean, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(165.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        StatusItem(onClick = {}, colorSuccessDefault, "Online")
        StatusItem(onClick = {}, grayscale3, "Offline")
        StatusItem(onClick = {}, errorDefault, "Busy")
    }
}

@Composable
fun StatusItem(onClick: () -> Unit, color: Color, text: String) {
    DropdownMenuItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color))
            Spacer(Modifier.width(7.dp))
            Text(text, color = Color.Black)
        }
    }
}

@Preview
@Composable
fun StatusItemPreview() {
    StatusItem({}, Color.Red, "Busy")
}

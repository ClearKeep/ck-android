package com.clearkeep.screen.chat.home.composes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.screen.chat.home.HomeViewModel
import com.clearkeep.screen.chat.profile.LogoutConfirmDialog

@Composable
fun SiteMenuScreen(
    homeViewModel: HomeViewModel,
    profile: Profile,
    closeSiteMenu: (() -> Unit),
    onLogout: (()->Unit),
    onNavigateServerSetting: () -> Unit,
    onNavigateAccountSetting: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateInvite: () -> Unit,
    onNavigateBannedUser: () -> Unit
) {
    val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(grayscaleOverlay)
            .focusable()
            .clickable(enabled = true, onClick = {  })
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
                            HeaderSite(profile, homeViewModel)
                            Spacer(modifier = Modifier.size(12.dp))
                            Divider(color = grayscale3)
                            SettingGeneral(onNavigateAccountSetting)
                            Divider(color = grayscale3)
                            SettingServer(
                                onNavigateServerSetting,
                                onNavigateNotificationSetting,
                                onNavigateInvite,
                                onNavigateBannedUser
                            )
                        }
                    }

                    Row(modifier = Modifier
                        .padding(top = 38.dp, bottom = 38.dp)
                        .align(Alignment.BottomCenter)
                        .clickable { onLogout() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = null,
                            tint = errorDefault
                        )
                        Text(
                            text = stringResource(R.string.logout), modifier = Modifier
                                .padding(start = 16.dp), style = TextStyle(
                                color = errorDefault,
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
fun HeaderSite(profile: Profile, homeViewModel: HomeViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val statusUse = homeViewModel.currentStatus.observeAsState()

    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarSite(url = profile.avatar, name = profile.userName ?: "", status = "", cacheKey = profile.updatedAt.toString())
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CKHeaderText(
                text = profile.userName ?: "",
                headerTextType = HeaderTextType.Normal,
                color = primaryDefault
            )
            Row(modifier = Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                when(statusUse.value){
                    UserStatus.ONLINE.value->{
                        Text(
                            text = UserStatus.ONLINE.value,
                            style = TextStyle(color = colorSuccessDefault, fontSize = 14.sp)
                        )
                    }
                    UserStatus.OFFLINE.value->{
                        Text(
                            text = UserStatus.OFFLINE.value,
                            style = TextStyle(color = grayscale3, fontSize = 14.sp)
                        )
                    }
                    else ->{
                        Text(
                            text = UserStatus.BUSY.value,
                            style = TextStyle(color = errorDefault, fontSize = 14.sp)
                        )
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chev_down),
                        null,
                        alignment = Alignment.Center
                    )
                }
            }

            StatusDropdown(expanded, onDismiss = { expanded = false },{
                expanded = false
                homeViewModel.setUserStatus(it)
            })

            ConstraintLayout(Modifier.fillMaxWidth()) {
                val text = createRef()
                val image = createRef()
                Text("Url: ${homeViewModel.getProfileLink()}", overflow = TextOverflow.Ellipsis, maxLines = 1, fontSize = 12.sp, modifier = Modifier.constrainAs(text){
                    linkTo(parent.start, image.start, endMargin = 4.dp)
                    width = Dimension.fillToConstraints
                })
                IconButton(
                    onClick = { copyProfileLinkToClipBoard(context, "profile link", homeViewModel.getProfileLink()) },
                    Modifier
                        .size(18.dp)
                        .constrainAs(image) {
                            end.linkTo(parent.end)
                        }
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
}

@Composable
fun SettingServer(
    onNavigateServerSetting: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateInvite: () -> Unit,
    onNavigateBannedUser: () -> Unit
) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        CKHeaderText(
            text = "Server Setting",
            headerTextType = HeaderTextType.Normal,
            color = grayscale2
        )
        ItemSiteSetting(stringResource(R.string.server), R.drawable.ic_adjustment, {
            onNavigateServerSetting()
        })
        ItemSiteSetting(stringResource(R.string.notification), R.drawable.ic_server_notification, {
            onNavigateNotificationSetting()
        })
        ItemSiteSetting(stringResource(R.string.invite), R.drawable.ic_user_plus, {
            onNavigateInvite()
        })
        ItemSiteSetting(stringResource(R.string.blocked), R.drawable.ic_user_off, {
            onNavigateBannedUser()
        })
    }
}

@Composable
fun SettingGeneral(
    onNavigateAccountSetting: () -> Unit,
) {
    Column() {
        ItemSiteSetting("Profile", R.drawable.ic_user, onNavigateAccountSetting)
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
fun StatusDropdown(expanded: Boolean, onDismiss: () -> Unit,statusChoose: (UserStatus)->Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(165.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        StatusItem(
            onClick = { statusChoose.invoke(UserStatus.ONLINE) },
            colorSuccessDefault,
            UserStatus.ONLINE.value
        )
        StatusItem(
            onClick = { statusChoose.invoke(UserStatus.BUSY) },
            errorDefault,
            UserStatus.BUSY.value
        )
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

private fun copyProfileLinkToClipBoard(context: Context, label: String, text: String) {
    val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "You copied", Toast.LENGTH_SHORT).show()
}


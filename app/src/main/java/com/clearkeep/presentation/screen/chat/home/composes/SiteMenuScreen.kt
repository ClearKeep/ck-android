package com.clearkeep.presentation.screen.chat.home.composes

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.presentation.components.*
import com.clearkeep.presentation.components.base.CKAlertDialog
import com.clearkeep.presentation.components.base.CKHeaderText
import com.clearkeep.presentation.components.base.HeaderTextType
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.UserStatus
import com.clearkeep.presentation.screen.chat.home.HomeViewModel
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun SiteMenuScreen(
    homeViewModel: HomeViewModel,
    profile: com.clearkeep.domain.model.Profile,
    closeSiteMenu: (() -> Unit),
    onLeaveServer: (() -> Unit),
    onNavigateServerSetting: () -> Unit,
    onNavigateAccountSetting: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateInvite: () -> Unit,
    onNavigateBannedUser: () -> Unit
) {
    val (showReminder, setShowReminderDialog) = rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(grayscaleOverlay)
            .focusable()
            .clickable(enabled = true, onClick = { })
    ) {
        Row(
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = LocalColorMapping.current.backgroundBrush
                ),
                alpha = if (LocalColorMapping.current.isDarkTheme) 1f else .4f
            )
        ) {
            Box(
                Modifier
                    .width(108.sdp())
            ) {
            }
            Card(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.sdp(), bottom = 20.sdp()),
                backgroundColor = LocalColorMapping.current.surface,
                shape = RoundedCornerShape(topStart = 30.sdp(), bottomStart = 30.sdp()),
                elevation = 8.sdp()
            ) {
                ConstraintLayout {
                    val (scrollContent, leaveServerButton) = createRefs()
                    Column(Modifier.constrainAs(scrollContent) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(leaveServerButton.top)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.sdp(),
                                    top = 24.sdp(),
                                    end = 16.sdp(),
                                    bottom = 20.sdp()
                                )
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    onClick = { closeSiteMenu.invoke() },
                                    Modifier.size(24.sdp())
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "",
                                        tint = LocalColorMapping.current.iconColorAlt,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(16.sdp()))
                            HeaderSite(profile, homeViewModel)
                            Spacer(modifier = Modifier.size(12.sdp()))
                            Divider(color = grayscale3)
                            SettingGeneral(onNavigateAccountSetting)
                            SettingServer(
                                onNavigateServerSetting,
                                onNavigateNotificationSetting
                            )
                        }
                    }

                    Row(modifier = Modifier
                        .padding(top = 8.sdp(), bottom = 38.sdp())
                        .wrapContentSize()
                        .constrainAs(leaveServerButton) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .clickable { setShowReminderDialog(true) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = null,
                            tint = LocalColorMapping.current.error,
                            modifier = Modifier.size(24.sdp())
                        )
                        Text(
                            text = stringResource(R.string.sign_out), modifier = Modifier
                                .padding(start = 16.sdp()), style = TextStyle(
                                color = LocalColorMapping.current.error,
                                fontSize = defaultNonScalableTextSize(),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        LeaveServerConfirmDialog(showReminder, setShowReminderDialog, onLeaveServer)
    }
}

@Composable
fun HeaderSite(profile: com.clearkeep.domain.model.Profile, homeViewModel: HomeViewModel) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val statusUse = homeViewModel.currentStatus.observeAsState()

    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarSite(
            url = profile.avatar,
            name = profile.userName ?: "",
            status = "",
            cacheKey = profile.updatedAt.toString()
        )
        Column(modifier = Modifier.padding(horizontal = 16.sdp())) {
            CKHeaderText(
                text = profile.userName ?: "",
                headerTextType = HeaderTextType.Normal,
                color = primaryDefault
            )
            Row(
                modifier = Modifier.clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (statusUse.value) {
                    com.clearkeep.domain.model.UserStatus.ONLINE.value -> {
                        Text(
                            text = com.clearkeep.domain.model.UserStatus.ONLINE.value,
                            style = TextStyle(
                                color = colorSuccessDefault,
                                fontSize = defaultNonScalableTextSize()
                            )
                        )
                    }
                    com.clearkeep.domain.model.UserStatus.OFFLINE.value, com.clearkeep.domain.model.UserStatus.UNDEFINED.value -> {
                        Text(
                            text = com.clearkeep.domain.model.UserStatus.OFFLINE.value,
                            style = TextStyle(
                                color = grayscale3,
                                fontSize = defaultNonScalableTextSize()
                            )
                        )
                    }
                    else -> {
                        Text(
                            text = com.clearkeep.domain.model.UserStatus.BUSY.value,
                            style = TextStyle(
                                color = errorDefault,
                                fontSize = defaultNonScalableTextSize()
                            )
                        )
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 8.sdp())) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chev_down),
                        null,
                        alignment = Alignment.Center,
                        modifier = Modifier.size(12.sdp()),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                }
            }

            StatusDropdown(expanded, onDismiss = { expanded = false }, {
                expanded = false
                homeViewModel.setUserStatus(it)
            })

            ConstraintLayout(Modifier.fillMaxWidth()) {
                val text = createRef()
                val image = createRef()
                Text(
                    homeViewModel.getProfileLink(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 12.sdp().toNonScalableTextSize(),
                    modifier = Modifier.constrainAs(text) {
                        linkTo(parent.start, image.start, endMargin = 4.dp)
                        width = Dimension.fillToConstraints
                    },
                    color = LocalColorMapping.current.profileText
                )
                IconButton(
                    onClick = {
                        copyProfileLinkToClipBoard(
                            context,
                            "profile link",
                            homeViewModel.getProfileLink()
                        )
                    },
                    Modifier
                        .size(18.sdp())
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
    onNavigateNotificationSetting: () -> Unit
) {
    Column(Modifier.padding(bottom = 16.sdp())) {
        ItemSiteSetting(stringResource(R.string.server), R.drawable.ic_adjustment, {
            onNavigateServerSetting()
        })
        ItemSiteSetting(stringResource(R.string.notification), R.drawable.ic_server_notification, {
            onNavigateNotificationSetting()
        })
    }
}

@Composable
fun SettingGeneral(
    onNavigateAccountSetting: () -> Unit,
) {
    Column {
        ItemSiteSetting(
            stringResource(R.string.profile),
            R.drawable.ic_user,
            onNavigateAccountSetting
        )
    }
}

@Composable
fun ItemSiteSetting(
    name: String,
    icon: Int,
    onClickAction: (() -> Unit)? = null,
    textColor: Color? = null
) {
    Row(
        modifier = Modifier
            .padding(top = 16.sdp(), bottom = 18.sdp())
            .clickable { onClickAction?.invoke() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.sdp()),
            tint = LocalColorMapping.current.iconColorAlt
        )
        SideBarLabel(
            text = name, color = LocalColorMapping.current.inputLabel, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.sdp())
        )
    }
}

@Composable
fun StatusDropdown(expanded: Boolean, onDismiss: () -> Unit, statusChoose: (com.clearkeep.domain.model.UserStatus) -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(165.sdp())
            .background(LocalColorMapping.current.surfaceDialog, RoundedCornerShape(8.sdp()))
    ) {
        StatusItem(
            onClick = { statusChoose.invoke(com.clearkeep.domain.model.UserStatus.ONLINE) },
            colorSuccessDefault,
            com.clearkeep.domain.model.UserStatus.ONLINE.value
        )
        StatusItem(
            onClick = { statusChoose.invoke(com.clearkeep.domain.model.UserStatus.BUSY) },
            errorDefault,
            com.clearkeep.domain.model.UserStatus.BUSY.value
        )
    }
}

@Composable
fun StatusItem(onClick: () -> Unit, color: Color, text: String) {
    DropdownMenuItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.sdp())
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(7.sdp()))
            Text(text, color = Color.Black)
        }
    }
}

@Composable
fun LeaveServerConfirmDialog(
    showReminder: Boolean,
    setShowDialog: (Boolean) -> Unit,
    onLeaveServer: () -> Unit,
) {
    if (showReminder) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = stringResource(R.string.sign_out_dialog),
            dismissTitle = stringResource(R.string.cancel),
            confirmTitle = stringResource(R.string.sign_out),
            onDismissButtonClick = {
                setShowDialog(false)
            },
            onConfirmButtonClick = {
                setShowDialog(false)
                onLeaveServer.invoke()
            },
        )
    }
}

private fun copyProfileLinkToClipBoard(context: Context, label: String, text: String) {
    val clipboard: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
}


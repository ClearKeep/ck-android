package com.clearkeep.screen.chat.room.room_detail

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import com.clearkeep.R
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.errorDefault
import com.clearkeep.components.grayscale1
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.FriendListItemInfo
import com.clearkeep.screen.chat.composes.FriendListMoreItem
import com.clearkeep.screen.chat.home.composes.SideBarLabel
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.sdp

@Composable
fun RoomInfoScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    val confirmLeaveGroupDialogVisible = remember { mutableStateOf(false) }

    groupState?.value?.let { group ->
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 8.sdp(), top = 24.sdp())
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack(
                                navHostController.graph.startDestination,
                                false
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chev_left),
                            contentDescription = ""
                        )
                    }
                    CKHeaderText(
                        group.groupName,
                        headerTextType = HeaderTextType.Medium,
                        modifier = Modifier
                            .weight(1.0f, true)
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pencil),
                                contentDescription = ""
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.sdp()))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val maxItems = 4
                    Box(modifier = Modifier.wrapContentWidth()) {
                        for (i in group.clientList.indices) {
                            if (i < maxItems) {
                                FriendListItemInfo(group.clientList[i], null, 28 * i)
                            } else {
                                FriendListMoreItem(group.clientList.size - maxItems, 28 * i)
                                break
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.sdp()))
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                roomViewModel.requestCall(group.groupId, true)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_call_audio),
                            contentDescription = null, modifier = Modifier
                                .size(36.sdp())
                        )
                        SideBarLabel(text = "Audio", color = primaryDefault)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                roomViewModel.requestCall(group.groupId, false)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_video_call),
                            contentDescription = null,
                            modifier = Modifier.size(36.sdp())
                        )
                        SideBarLabel(text = "Video", color = primaryDefault)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_alert),
                            contentDescription = null,
                            modifier = Modifier.size(36.sdp())
                        )
                        SideBarLabel(text = "Mute", color = primaryDefault)
                    }

                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.sdp())
                ) {
                    Spacer(modifier = Modifier.height(20.sdp()))
                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "See Members",
                                R.drawable.ic_user,
                                textColor = grayscale1,
                                onClickAction = {
                                    roomViewModel.getStatusUserInGroup()
                                    navHostController.navigate("member_group_screen")
                                })
                        }
                    }

                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "Add member",
                                R.drawable.ic_user_plus,
                                textColor = grayscale1,
                                onClickAction = {
                                    navHostController.navigate("invite_group_screen")
                                })
                        }
                    }

                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "Remove member",
                                R.drawable.ic_user_off,
                                textColor = grayscale1,
                                onClickAction = {
                                    navHostController.navigate("remove_member")
                                })
                        }

                    }
                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "Leave Group",
                                R.drawable.ic_logout,
                                textColor = errorDefault,
                                onClickAction = {
                                    confirmLeaveGroupDialogVisible.value = true
                                })
                        }
                    }
                }
            }

            if (confirmLeaveGroupDialogVisible.value) {
                CKAlertDialog(
                    title = stringResource(R.string.warning),
                    text = "Are you sure you want to leave ${group.groupName}?",
                    confirmTitle = "Leave group chat",
                    dismissTitle = stringResource(R.string.cancel),
                    onConfirmButtonClick = {
                        confirmLeaveGroupDialogVisible.value = false
                        roomViewModel.leaveGroup()
                    },
                    onDismissButtonClick = {
                        confirmLeaveGroupDialogVisible.value = false
                    }
                )
            }
        }
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
            .padding(top = 16.sdp())
            .clickable { onClickAction?.invoke() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.sdp()))
        SideBarLabel(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.sdp())
        )
    }
}
package com.clearkeep.presentation.screen.chat.room.roomdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.clearkeep.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.CKAlertDialog
import com.clearkeep.common.presentation.components.base.CKHeaderText
import com.clearkeep.common.presentation.components.base.HeaderTextType
import com.clearkeep.common.presentation.components.primaryDefault
import com.clearkeep.presentation.screen.chat.composes.FriendListItemInfo
import com.clearkeep.presentation.screen.chat.composes.FriendListMoreItem
import com.clearkeep.presentation.screen.chat.home.composes.SideBarLabel
import com.clearkeep.presentation.screen.chat.room.RoomViewModel
import com.clearkeep.common.utilities.sdp

@Composable
fun RoomInfoScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    val confirmLeaveGroupDialogVisible = rememberSaveable {mutableStateOf(false) }

    groupState.value?.let { group ->
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
                                navHostController.graph.findStartDestination().id,
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
                }
                Spacer(modifier = Modifier.height(20.sdp()))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val maxItems = 4
                    Box(modifier = Modifier.wrapContentWidth()) {
                        val listUserStatusState = roomViewModel.listGroupUserStatus.observeAsState()
                        for (i in group.clientList.indices) {
                            if (i < maxItems) {
                                FriendListItemInfo(
                                    listUserStatusState.value?.get(i) ?: group.clientList[i],
                                    null,
                                    28 * i
                                )
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
                        SideBarLabel(text = stringResource(R.string.audio), color = primaryDefault)
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
                        SideBarLabel(text = stringResource(R.string.video), color = primaryDefault)
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
                                stringResource(R.string.see_members),
                                R.drawable.ic_user,
                                textColor = LocalColorMapping.current.inputLabel,
                                iconTint = LocalColorMapping.current.inputLabel,
                                onClickAction = {
                                    //roomViewModel.getStatusUserInGroup()
                                    navHostController.navigate("member_group_screen")
                                })
                        }
                    }

                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                stringResource(R.string.add_member),
                                R.drawable.ic_user_plus,
                                textColor = LocalColorMapping.current.inputLabel,
                                iconTint = LocalColorMapping.current.inputLabel,
                                onClickAction = {
                                    navHostController.navigate("invite_group_screen")
                                })
                        }
                    }

                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                stringResource(R.string.remove_member),
                                R.drawable.ic_user_off,
                                textColor = LocalColorMapping.current.inputLabel,
                                iconTint = LocalColorMapping.current.inputLabel,
                                onClickAction = {
                                    navHostController.navigate("remove_member")
                                })
                        }

                    }
                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                stringResource(R.string.room_info_leave),
                                R.drawable.ic_logout,
                                iconTint = LocalColorMapping.current.error,
                                textColor = LocalColorMapping.current.error,
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
                    text = stringResource(
                        R.string.room_info_confirm_leave_dialog_text,
                        group.groupName
                    ),
                    confirmTitle = stringResource(R.string.room_info_confirm_leave_dialog_confirm),
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
    iconTint: Color? = null,
    onClickAction: (() -> Unit)? = null,
    textColor: Color? = null
) {
    Row(
        modifier = Modifier
            .padding(top = 16.sdp())
            .clickable { onClickAction?.invoke() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.sdp()),
            colorFilter = if (iconTint != null) ColorFilter.tint(iconTint) else null
        )
        SideBarLabel(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.sdp())
        )
    }
}
package com.clearkeep.screen.chat.group_remove

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.CKSearchBox
import com.clearkeep.components.base.CKText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserStateTypeInGroup
import com.clearkeep.screen.chat.composes.*
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.sdp

@Composable
fun RemoveMemberScreen(roomViewModel: RoomViewModel, navController: NavController) {
    val text = remember { mutableStateOf("") }
    val groupState = roomViewModel.group.observeAsState()
    val context = LocalContext.current
    val removeMemberDialogVisible = remember { mutableStateOf(false) }
    val confirmRemoveMemberData = remember { mutableStateOf<Pair<User, Long>?>(null) }

    groupState.value?.let { group ->
        CKSimpleTheme {
            Surface(color = MaterialTheme.colors.background) {
                Column(
                    Modifier
                        .fillMaxSize()
                ) {
                    HeaderRemoveMember {
                        navController.popBackStack()
                    }
                    Spacer(modifier = Modifier.height(24.sdp()))

                    Column(
                        Modifier.padding(horizontal = 16.sdp())
                    ) {
                        CKSearchBox(
                            text,
                            Modifier
                                .background(grayscale5, RoundedCornerShape(16.sdp()))
                        )
                        Spacer(modifier = Modifier.height(16.sdp()))
                        CKText("User in this Group Chat", color = grayscale2)
                        Spacer(modifier = Modifier.height(16.sdp()))

                        val itemModifier = Modifier.padding(vertical = 8.sdp())
                        LazyColumn {
                            itemsIndexed(group.clientList.filter {
                                it.userState == UserStateTypeInGroup.ACTIVE.value && it != roomViewModel.getCurrentUser()
                                        && it.userName.toLowerCase().contains(text.value)
                            }.sortedBy { it.userName.toLowerCase() }) { _, user ->
                                RemoveMemberItem(itemModifier, user) {
                                    roomViewModel.group.value?.groupId?.let { it1 ->
                                        confirmRemoveMemberData.value = user to it1
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (removeMemberDialogVisible.value) {
        CKAlertDialog(
            title = stringResource(R.string.success),
            text = "Remove member success",
            onDismissButtonClick = {
                removeMemberDialogVisible.value = false
            }
        )
    }

    if (confirmRemoveMemberData.value != null) {
        val user = confirmRemoveMemberData.value!!.first
        val groupId = confirmRemoveMemberData.value!!.second
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = "Do you want to remove ${user.userName} from the group chat?",
            confirmTitle = stringResource(R.string.remove),
            dismissTitle = stringResource(R.string.cancel),
            onConfirmButtonClick = {
                confirmRemoveMemberData.value = null
                roomViewModel.removeMember(user, groupId = groupId, onSuccess = {
                    removeMemberDialogVisible.value = true
                },onError = {
                    Toast.makeText(context,"Remove member error !",Toast.LENGTH_LONG).show()
                })
            },
            onDismissButtonClick = {
                confirmRemoveMemberData.value = null
            }
        )
    }
}

@Composable
fun HeaderRemoveMember(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(24.sdp()))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        onCloseView.invoke()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chev_left),
                        contentDescription = null,
                        tint = grayscale1,
                    )
                }
                CKHeaderText(
                    "Remove Member", modifier = Modifier
                        .weight(1.0f, true), headerTextType = HeaderTextType.Medium
                )
            }
    }
}

@Composable
fun RemoveMemberItem(
    modifier: Modifier = Modifier,
    user: User,
    onAction: (user: User) -> Unit
) {
    NewFriendListItem(modifier, user, { StatusText(user) }, {
        Icon(
            painter = painterResource(id = R.drawable.ic_cross),
            contentDescription = "",
            tint = errorDefault,
            modifier = Modifier.clickable {
                onAction(user)
            }
        )
    })
}
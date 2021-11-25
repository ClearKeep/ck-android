package com.clearkeep.presentation.screen.chat.groupremove

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.clearkeep.R
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.CKAlertDialog
import com.clearkeep.common.presentation.components.base.CKHeaderText
import com.clearkeep.common.presentation.components.base.CKSearchBox
import com.clearkeep.common.presentation.components.base.HeaderTextType
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserStateTypeInGroup
import com.clearkeep.presentation.screen.chat.composes.*
import com.clearkeep.presentation.screen.chat.room.RoomViewModel
import com.clearkeep.common.utilities.sdp
import java.util.*

@Composable
fun RemoveMemberScreen(roomViewModel: RoomViewModel, navController: NavController) {
    val text = rememberSaveable { mutableStateOf("") }
    val groupState = roomViewModel.group.observeAsState()
    val context = LocalContext.current
    val removeMemberDialogVisible = rememberSaveable { mutableStateOf(false) }
    val confirmRemoveMemberData = remember { mutableStateOf<Pair<com.clearkeep.domain.model.User, Long>?>(null) }

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
                        Spacer(modifier = Modifier.height(32.sdp()))

                        val itemModifier = Modifier.padding(vertical = 8.sdp())
                        val usersList = group.clientList.filter {
                            it.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value
                                    && it.userId != roomViewModel.getCurrentUser().userId
                                    && it.userName.toLowerCase(Locale.ROOT).contains(text.value)
                        }.sortedBy { it.userName.toLowerCase(Locale.ROOT) }

                        LazyColumn {
                            itemsIndexed(usersList) { _, user ->
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
            text = stringResource(R.string.remove_member_success_text),
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
            text = stringResource(R.string.remove_member_confirm_dialog_text, user.userName),
            confirmTitle = stringResource(R.string.remove),
            dismissTitle = stringResource(R.string.cancel),
            onConfirmButtonClick = {
                confirmRemoveMemberData.value = null
                roomViewModel.removeMember(user, groupId = groupId, onSuccess = {
                    removeMemberDialogVisible.value = true
                }, onError = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.remove_member_error),
                        Toast.LENGTH_LONG
                    ).show()
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
                    tint = LocalColorMapping.current.headerText,
                )
            }
            CKHeaderText(
                stringResource(R.string.remove_member), modifier = Modifier
                    .weight(1.0f, true), headerTextType = HeaderTextType.Medium
            )
        }
    }
}

@Composable
fun RemoveMemberItem(
    modifier: Modifier = Modifier,
    user: com.clearkeep.domain.model.User,
    onAction: (user: com.clearkeep.domain.model.User) -> Unit
) {
    NewFriendListItem(modifier, user, { StatusText(user) }, {
        Icon(
            painter = painterResource(id = R.drawable.ic_cross),
            contentDescription = "",
            tint = LocalColorMapping.current.error,
            modifier = Modifier.clickable {
                onAction(user)
            }
        )
    })
}
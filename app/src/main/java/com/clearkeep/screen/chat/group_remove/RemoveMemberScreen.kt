package com.clearkeep.screen.chat.group_remove

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.CKSearchBox
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.*
import com.clearkeep.screen.chat.room.RoomViewModel

@Composable
fun RemoveMemberScreen(roomViewModel: RoomViewModel, navController: NavController) {
    val text = remember { mutableStateOf("") }
    val groupState = roomViewModel.group.observeAsState()

    groupState?.value?.let { group ->
        CKSimpleTheme {
            Surface(color = MaterialTheme.colors.background) {
                Column(
                    Modifier
                        .fillMaxSize()
                ) {
                    HeaderRemoveMember {
                        navController.popBackStack()
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        Modifier.padding(horizontal = 16.dp)
                    ) {
                        CKSearchBox(
                            text,
                            Modifier
                                .background(grayscale5, RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("User in this Group Chat", color = grayscale2)
                        Spacer(modifier = Modifier.height(16.dp))

                        val itemModifier = Modifier.padding(vertical = 8.dp)


                        LazyColumn {
                            itemsIndexed(group.clientList) { _, user ->
                                RemoveMemberItem(itemModifier, user) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderRemoveMember(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(24.dp))
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
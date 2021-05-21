package com.clearkeep.screen.chat.room.room_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.screen.chat.composes.FriendListItem
import com.clearkeep.screen.chat.room.RoomViewModel

@Composable
fun GroupMemberScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    groupState?.value?.let { group ->
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column {
                CKTopAppBar(
                    title = {
                        Text(text = "Member")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navHostController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = ""
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.height(30.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
                ) {
                    itemsIndexed(group.clientList) { _, friend ->
                        FriendListItem(friend)
                    }
                }
            }
        }
    }
}
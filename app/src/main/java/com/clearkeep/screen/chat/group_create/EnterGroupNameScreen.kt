package com.clearkeep.screen.chat.group_create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscaleBlack
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.screen.chat.composes.FriendListItem

@Composable
fun EnterGroupNameScreen(
    navController: NavHostController,
    createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = remember {mutableStateOf("")}
    val createGroupState = createGroupViewModel.createGroupState.observeAsState()
    val friends = createGroupViewModel.invitedFriends

    Surface(
        color = grayscaleOffWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.0f, true),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = {
                            navController.popBackStack(navController.graph.startDestination, false)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "",
                            tint = grayscale1
                        )
                    }
                }

                CKTextButton(
                    title = "Create",
                    onClick = { createGroupViewModel.createGroup(groupName.value.trim()) },
                    enabled = groupName.value.isNotBlank() && createGroupState.value != CreateGroupProcessing,
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
            Spacer(modifier = Modifier.height(25.dp))
            Text("New Group Message", style = MaterialTheme.typography.h5.copy(
                color = grayscaleBlack,
            ))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Group Name", style = MaterialTheme.typography.body2.copy(
                color = grayscale1,
            ))
            CKTextInputField(
                "Name this group",
                groupName
            )
            Spacer(modifier = Modifier.height(16.dp))
            friends.let { values ->
                LazyColumn(
                    contentPadding = PaddingValues(end = 16.dp),
                ) {
                    itemsIndexed(values) { _, friend ->
                        FriendListItem(friend)
                    }
                }
            }
        }
    }
}
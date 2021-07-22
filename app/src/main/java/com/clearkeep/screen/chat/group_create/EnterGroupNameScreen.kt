package com.clearkeep.screen.chat.group_create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale1
import com.clearkeep.screen.chat.composes.FriendListItem

@Composable
fun EnterGroupNameScreen(
    navController: NavHostController,
    createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = remember {mutableStateOf("")}
    val createGroupState = createGroupViewModel.createGroupState.observeAsState()
    val friends = createGroupViewModel.invitedFriends
    val isLoadingState = remember { mutableStateOf(false) }
    Box() {
    Surface(
        color = MaterialTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(0.66f)
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1.0f, true),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                navController.popBackStack(
                                    navController.graph.startDestination,
                                    false
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chev_left),
                                contentDescription = "",
                                tint = grayscale1
                            )
                        }
                        CKHeaderText("Create group", headerTextType = HeaderTextType.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                CKHeaderText("Group Name", headerTextType = HeaderTextType.Normal)
                Spacer(modifier = Modifier.height(16.dp))
                CKTextInputField(
                    "Name this group",
                    groupName
                )
                Spacer(modifier = Modifier.height(16.dp))
                CKHeaderText("User in this group", headerTextType = HeaderTextType.Normal)
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CKButton(
                    stringResource(R.string.btn_next),
                    onClick = {
                        if (groupName.value.trim().isNotEmpty()) {
                            createGroupViewModel.createGroup(groupName.value.trim(),onError = {
                                isLoadingState.value = false
                            })
                            isLoadingState.value = true
                        }
                    },
                    modifier = Modifier
                        .width(200.dp)
                )
            }
        }
    }
        isLoadingState.value.let {
            if (it) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                    CKCircularProgressIndicator(
                        color = Color.Blue
                    )
                }
            }
        }
    }

}
package com.clearkeep.presentation.screen.chat.groupcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.clearkeep.utilities.sdp
import androidx.navigation.NavHostController
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.*
import com.clearkeep.presentation.screen.chat.composes.FriendListItem

@Composable
fun EnterGroupNameScreen(
    navController: NavHostController,
    createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = remember { mutableStateOf("") }
    val createGroupState = createGroupViewModel.createGroupState.observeAsState()
    val friends = createGroupViewModel.invitedFriends
    val isLoadingState = rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.sdp())
                        .weight(0.66f)
                ) {
                    Row(
                        modifier = Modifier.padding(end = 8.sdp(), top = 24.sdp()),
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
                                        navController.graph.findStartDestination().id,
                                        false
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chev_left),
                                    contentDescription = "",
                                    tint = LocalColorMapping.current.iconColorAlt
                                )
                            }
                            CKHeaderText(
                                stringResource(R.string.create_group),
                                headerTextType = HeaderTextType.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.sdp()))
                    CKHeaderText(
                        stringResource(R.string.create_group_group_name),
                        headerTextType = HeaderTextType.Normal
                    )
                    Spacer(modifier = Modifier.height(16.sdp()))
                    CKTextInputField(
                        stringResource(R.string.create_group_group_name_placeholder),
                        groupName
                    )
                    Spacer(modifier = Modifier.height(16.sdp()))
                    friends.let { values ->
                        LazyColumn(
                            contentPadding = PaddingValues(end = 16.sdp()),
                        ) {
                            item {
                                CKHeaderText(
                                    stringResource(R.string.create_group_users),
                                    headerTextType = HeaderTextType.Normal,
                                    color = LocalColorMapping.current.bodyTextDisabled
                                )
                                Spacer(modifier = Modifier.height(16.sdp()))
                            }

                            itemsIndexed(values) { _, friend ->
                                FriendListItem(friend = friend)
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(16.sdp())
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CKButton(
                        stringResource(R.string.create),
                        onClick = {
                            if (groupName.value.trim().isNotEmpty()) {
                                createGroupViewModel.createGroup(groupName.value.trim(), onError = {
                                    isLoadingState.value = false
                                })
                                isLoadingState.value = true
                            }
                        },
                        modifier = Modifier
                            .width(200.sdp()),
                        enabled = groupName.value.isNotBlank() && !isLoadingState.value
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
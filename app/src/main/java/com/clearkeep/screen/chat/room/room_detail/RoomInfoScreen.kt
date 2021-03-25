package com.clearkeep.screen.chat.room.room_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.clearkeep.screen.chat.home.composes.CircleAvatar
import com.clearkeep.screen.chat.room.RoomViewModel

@Composable
fun RoomInfoScreen(
        roomViewModel: RoomViewModel,
        navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    groupState?.value?.let { group ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(text = "${group.groupName} details")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack(navHostController.graph.startDestination, false)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleAvatar("", size = 72.dp)
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = group.groupName, style = MaterialTheme.typography.h5)
            }
            Spacer(modifier = Modifier.height(30.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = "Other features", style = MaterialTheme.typography.body1.copy(color = Color.Gray.copy(alpha = 0.8f)))
                Spacer(modifier = Modifier.height(20.dp))
                group.let {
                    if (it.isGroup()) {
                        Row(modifier = Modifier.clickable(onClick = {
                            navHostController.navigate("invite_group_screen")
                        }),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                    color = Color.Gray.copy(alpha = 0.8f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(30.dp, 30.dp).clickable(onClick = {
                                        navHostController.navigate("invite_group_screen")
                                    })
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = ""
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Add members", style = MaterialTheme.typography.body2)
                        }
                    }
                }
                group.let {
                    if (it.isGroup()) {
                        Row(modifier = Modifier.padding(top = 20.dp).clickable(onClick = {
                            navHostController.navigate("member_group_screen")
                        }),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                    color = Color.Gray.copy(alpha = 0.8f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(30.dp, 30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = ""
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Show members", style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
        }
    }
}
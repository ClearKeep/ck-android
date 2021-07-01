package com.clearkeep.screen.chat.room.room_detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import com.clearkeep.R
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.components.errorDefault
import com.clearkeep.components.grayscale1
import com.clearkeep.components.primaryDefault
import com.clearkeep.screen.chat.composes.FriendListItemInfo
import com.clearkeep.screen.chat.home.composes.SideBarLabel
import com.clearkeep.screen.chat.room.RoomViewModel

@Composable
fun RoomInfoScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {

    val groupState = roomViewModel.group.observeAsState()
    groupState?.value?.let { group ->
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp, top = 24.dp)
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
                        "${group.groupName}",
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
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyRow(
                        modifier = Modifier.wrapContentWidth(),

                        horizontalArrangement = Arrangement.Center
                    ) {
                        itemsIndexed(group.clientList) { index, friend ->
                            FriendListItemInfo(friend)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_call_audio),
                            contentDescription = null, modifier = Modifier
                                .width(36.dp)
                                .height(36.dp)
                        )
                        SideBarLabel(text = "Audio", color = primaryDefault)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_video_call),
                            contentDescription = null
                        )
                        SideBarLabel(text = "Video", color = primaryDefault)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_button_alert),
                            contentDescription = null
                        )
                        SideBarLabel(text = "Mute", color = primaryDefault)
                    }

                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "See Members",
                                R.drawable.ic_user,
                                textColor = grayscale1,
                                onClickAction = {
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
                                onClickAction = {})
                        }

                    }
                    group.let {
                        if (it.isGroup()) {
                            ItemSiteSetting(
                                "Leave Group",
                                R.drawable.ic_logout,
                                textColor = errorDefault,
                                onClickAction = {})

                        }
                    }
                }
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
            .padding(top = 16.dp)
            .clickable { onClickAction?.invoke() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(icon), contentDescription = null)
        SideBarLabel(
            text = name, color = textColor, modifier = Modifier
                .weight(0.66f)
                .padding(start = 16.dp)
        )
    }
}
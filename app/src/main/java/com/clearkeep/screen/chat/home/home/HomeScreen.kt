package com.clearkeep.screen.chat.home.home

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.home.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.home.home.composes.CircleAvatarWorkSpace

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    gotoSearch: (() -> Unit),
    createGroupChat: ((isDirectGroup: Boolean) -> Unit),
    gotoRoomById: ((idRoom: Long) -> Unit)
) {
    val rooms = homeViewModel.groups.observeAsState()
    Row(
        Modifier
            .fillMaxSize()
            .background(grayscaleOffWhite)
    ) {
        Box(
            Modifier
                .width(84.dp)
                .background(Color.White)
        ) {
            LeftMenu(homeViewModel)
        }
        Column(
            Modifier.fillMaxSize()
        ) {
            WorkSpaceView(homeViewModel, gotoSearch, createGroupChat,onItemClickListener = gotoRoomById)
        }
    }

}


@Composable
fun LeftMenu(mainViewModel: HomeViewModel) {
    val workSpaces = mainViewModel.servers.observeAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(0.66f)
                .background(
                    shape = RoundedCornerShape(topEnd = 30.dp),
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                ),horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.background(
                    color = grayscaleOverlay, shape = RoundedCornerShape(topEnd = 30.dp),
                ),horizontalAlignment = Alignment.CenterHorizontally
            ) {
                workSpaces.value?.let { item ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(
                            top = 20.dp,
                            end = 10.dp,
                            start = 10.dp,
                            bottom = 20.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, workSpace ->
                            Column(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(color = Color.Transparent)
                                    .border(
                                        BorderStroke(1.5.dp, primaryDefault),
                                        shape = CircleShape
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center

                            ) {
                                CircleAvatarWorkSpace(workSpace, mainViewModel.channelIdLive)
                            }
                        }

                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(1.dp))

        Column(
            modifier = Modifier
                .height(98.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                ),

            ) {
            Column(
                Modifier
                    .height(98.dp)
                    .fillMaxWidth()
                    .background(
                        color = grayscaleOverlay,
                    ), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_button_profile),
                    null,
                    Modifier.size(50.dp),
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
fun ItemListDirectMessage(chatGroup: ChatGroup, clintId: String,onItemClickListener: ((Long) -> Unit)? = null) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .clickable {
                onItemClickListener?.invoke(chatGroup.id)
            }
    ) {
        val roomName = if (chatGroup.isGroup()) chatGroup.groupName else {
            chatGroup.clientList.firstOrNull { client ->
                client.id != clintId
            }?.userName ?: ""
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleAvatarStatus(url = "", name = roomName, status = "")
            Text(
                text = roomName, modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(0.66f), style = TextStyle(
                    color = grayscale2, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            )
            /*Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Red
                            )
                        )
                    )
                    .size(24.dp)
            ) {
                Text(
                    text = "",
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )

            }*/
        }
    }
}


@Composable
fun ChatGroupItemView(chatGroup: ChatGroup, onItemClickListener: ((Long) -> Unit)? = null) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .clickable {
                onItemClickListener?.invoke(chatGroup.id)
            },
    ) {
        Row() {
            Text(
                text = chatGroup.groupName, modifier = Modifier
                    .weight(0.66f), style = TextStyle(
                    color = grayscale2, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            )
            /*Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Red
                            )
                        )
                    )
                    .size(24.dp)
            ) {
                Text(
                    text = chatGroup.toString(),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )

            }*/
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatGroupView(
    viewModel: HomeViewModel, createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    val chatGroups = viewModel.chatGroups.observeAsState()
    val rememberitemGroup = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.wrapContentHeight()
    ) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f),verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.clickable {
                    rememberitemGroup.value = !rememberitemGroup.value
                },verticalAlignment = Alignment.CenterVertically) {
                    CKHeaderText(
                        text = "Group Chat (${chatGroups.value?.size})",
                        headerTextType = HeaderTextType.Normal,color = grayscale2
                    )

                    Box(modifier = Modifier.padding(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(false)
                }
                .size(24.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center
                )
            }
        }
        Column() {
            AnimatedVisibility(
                visible = rememberitemGroup.value,
                enter = expandIn(
                    expandFrom = Alignment.BottomStart, initialSize = { IntSize(50, 50) },
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkOut(
                    shrinkTowards = Alignment.CenterStart,
                    targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 10) },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )

            ) {
                chatGroups.value?.let { item ->
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            start = 20.dp,
                        ),
                    ) {
                        itemsIndexed(item) { _, chatGroup ->
                            ChatGroupItemView(chatGroup,onItemClickListener)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DirectMessagesView(
    viewModel: HomeViewModel,
    createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    val chatGroupDummy = viewModel.directGroups.observeAsState()
    val rememberItemGroup = remember { mutableStateOf(true) }
    Column(modifier = Modifier.wrapContentHeight()) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier
                    .clickable {
                        rememberItemGroup.value = !rememberItemGroup.value
                    }, verticalAlignment = Alignment.CenterVertically) {
                    CKHeaderText(
                        text = "Direct Messages (${chatGroupDummy.value?.size})",
                        headerTextType = HeaderTextType.Normal,color = grayscale2
                    )
                    Box(modifier = Modifier.padding(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(true)
                }
                .size(24.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center
                )
            }
        }
        Column() {
            AnimatedVisibility(
                visible = rememberItemGroup.value,
                enter = expandIn(
                    expandFrom = Alignment.BottomStart, initialSize = { IntSize(50, 50) },
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkOut(
                    shrinkTowards = Alignment.CenterStart,
                    targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 10) },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )

            ) {
                chatGroupDummy.value?.let { item ->
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = 4.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, chatGroup ->
                            ItemListDirectMessage(chatGroup, viewModel.myClintId,onItemClickListener)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkSpaceView(
    homeViewModel: HomeViewModel,
    gotoSearch: () -> Unit,
    createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)?
) {
    val searchKey = remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 20.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Row(modifier = Modifier,verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                text = "CK Development", modifier = Modifier
                    .weight(0.66f), headerTextType = HeaderTextType.Large
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hamburger),
                    null, alignment = Alignment.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        CKTextInputField(
            stringResource(R.string.search),
            singleLine = true,
            leadingIcon = {
                Image(
                    painterResource(R.drawable.ic_search),
                    contentDescription = null
                )
            }, modifier = Modifier, textValue = searchKey
        )

        Spacer(modifier = Modifier.size(24.dp))
        NoteView()
        Spacer(modifier = Modifier.size(24.dp))
        ChatGroupView(
            homeViewModel, createGroupChat = createGroupChat,
            onItemClickListener = onItemClickListener
        )
        Spacer(modifier = Modifier.size(24.dp))
        DirectMessagesView(
            homeViewModel,
            createGroupChat = createGroupChat,
            onItemClickListener = onItemClickListener
        )
    }
}

@Composable
fun NoteView() {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_notes),
            contentDescription = null,
        )
        CKHeaderText(
            text = stringResource(R.string.notes), modifier = Modifier.padding(start = 10.dp), headerTextType = HeaderTextType.Normal,color = grayscale2,
        )
    }
}


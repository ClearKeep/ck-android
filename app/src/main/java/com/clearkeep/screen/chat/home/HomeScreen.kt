package com.clearkeep.screen.chat.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.home.composes.CircleAvatarWorkSpace
import com.clearkeep.screen.chat.home.composes.SiteMenuScreen
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    gotoSearch: () -> Unit,
    createGroupChat: ((isDirectGroup: Boolean) -> Unit),
    gotoRoomById: ((idRoom: Long) -> Unit),
    onSignOut: () -> Unit,
    onJoinServer: (serverUrl: String) -> Unit,
    onNavigateServerSetting: () -> Unit,
    onNavigateAccountSetting: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateInvite: () -> Unit,
    onNavigateBannedUser: () -> Unit,
    onNavigateNotes: () -> Unit,
) {
    val showJoinServer = homeViewModel.selectingJoinServer.observeAsState()
    val rememberStateSiteMenu = remember { mutableStateOf(false) }
    val profile = homeViewModel.profile.observeAsState()
    val swipeRefreshState = homeViewModel.isRefreshing.observeAsState()
    val serverUrlValidateResponse = homeViewModel.serverUrlValidateResponse.observeAsState()
    val isServerValidationLoading = homeViewModel.isServerUrlValidateLoading.observeAsState()

    SwipeRefresh(
        state = rememberSwipeRefreshState(swipeRefreshState.value == true),
        onRefresh = { homeViewModel.onPullToRefresh() }) {
        Row(
            Modifier
                .fillMaxSize()
        ) {
            Box(
                Modifier
                    .width(84.sdp())
            ) {
                LeftMenu(homeViewModel)
            }
            Column(
                Modifier.fillMaxSize()
            ) {
                if (showJoinServer.value == false) {
                    WorkSpaceView(
                        homeViewModel,
                        gotoSearch,
                        createGroupChat,
                        gotoProfile = {
                            rememberStateSiteMenu.value = true
                        },
                        onItemClickListener = {
                            gotoRoomById(it)
                        },
                        onNavigateNotes = onNavigateNotes
                    )
                } else {
                    JoinServerComposable(
                        onJoinServer = {
                            homeViewModel.checkValidServerUrl(it)
                        },
                        gotoProfile = {
                            homeViewModel.cancelCheckValidServer()
                            rememberStateSiteMenu.value = true
                        },
                        isLoading = isServerValidationLoading.value == true
                    )
                }
            }
        }
    }
    AnimatedVisibility(
        visible = rememberStateSiteMenu.value,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 3 },
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { 200 },
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut()

    ) {
        Column(
            Modifier
                .fillMaxSize()
                .focusable(true)

        ) {
            profile.value?.let {
                printlnCK("profile = ${it.userName}")
                SiteMenuScreen(
                    homeViewModel,
                    it,
                    closeSiteMenu = {
                        rememberStateSiteMenu.value = false
                    },
                    onLeaveServer = {
                        rememberStateSiteMenu.value = false
                        onSignOut.invoke()
                    },
                    onNavigateServerSetting = onNavigateServerSetting,
                    onNavigateAccountSetting = onNavigateAccountSetting,
                    onNavigateNotificationSetting = onNavigateNotificationSetting,
                    onNavigateBannedUser = onNavigateBannedUser,
                    onNavigateInvite = onNavigateInvite,
                )
            }
        }
    }
    if (serverUrlValidateResponse.value?.isBlank() == true) {
        CKAlertDialog(
            title = stringResource(R.string.error),
            text = stringResource(R.string.wrong_server_url_error),
            onDismissButtonClick = {
                homeViewModel.serverUrlValidateResponse.value = null
            },
            dismissTitle = stringResource(R.string.close)
        )
    } else if (serverUrlValidateResponse.value != null) {
        onJoinServer(serverUrlValidateResponse.value!!)
        homeViewModel.serverUrlValidateResponse.value = null
    }
}


@Composable
fun LeftMenu(mainViewModel: HomeViewModel) {
    val workSpaces = mainViewModel.servers.observeAsState()
    val selectingJoinServer = mainViewModel.selectingJoinServer.observeAsState()

    workSpaces.value?.let { serverList ->
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1.0f, true)
                    .padding(top = 20.sdp())
                    .background(
                        shape = RoundedCornerShape(topEnd = 30.sdp()),
                        brush = Brush.horizontalGradient(
                            //todo disable dark mode
                            colors = listOf(
                                if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                                if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                            )
                        )
                    ), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = grayscaleOverlay,
                            shape = RoundedCornerShape(topEnd = 30.sdp()),
                        ), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(
                            top = 20.sdp(),
                            end = 10.sdp(),
                            start = 10.sdp(),
                        ),
                    ) {
                        itemsIndexed(serverList) { _, server ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center

                            ) {
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            mainViewModel.selectChannel(server)
                                            mainViewModel.cancelCheckValidServer()
                                        },
                                ) {
                                    CircleAvatarWorkSpace(
                                        server,
                                        server.isActive && selectingJoinServer.value != true
                                    )
                                }
                                Spacer(modifier = Modifier.height(36.sdp()))
                            }
                        }
                    }
                    AddWorkspace(mainViewModel)
                }
            }
        }
    }

}


@Composable
fun ItemListDirectMessage(
    modifier: Modifier,
    chatGroup: ChatGroup,
    listUserStatus: List<User>?,
    clintId: String,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable {
                onItemClickListener?.invoke(chatGroup.groupId)
            }
    ) {
        val partnerUser = chatGroup.clientList.firstOrNull { client ->
            client.userId != clintId
        }
        val roomName =
            if (chatGroup.isDeletedUserPeer) stringResource(R.string.deleted_user) else partnerUser?.userName
                ?: ""
        val userStatus = listUserStatus?.firstOrNull { client ->
            client.userId == partnerUser?.userId
        }?.userStatus ?: ""
        val avatar = listUserStatus?.firstOrNull { client ->
            client.userId == partnerUser?.userId
        }?.avatar ?: ""

        Row(
            modifier = Modifier.padding(top = 16.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatarStatus(url = avatar, name = roomName, status = userStatus)
            Text(
                text = roomName, modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.sdp()),
                maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = defaultNonScalableTextSize(),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


@Composable
fun ChatGroupItemView(
    modifier: Modifier,
    chatGroup: ChatGroup,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable {
                onItemClickListener?.invoke(chatGroup.groupId)
            },
    ) {
        Row(modifier = Modifier.padding(top = 16.sdp())) {
            Text(
                text = if (chatGroup.isDeletedUserPeer) stringResource(R.string.deleted_user) else chatGroup.groupName,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = defaultNonScalableTextSize(),
                    fontWeight = FontWeight.Bold
                )
            )
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
    val rememberItemGroup = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.wrapContentHeight()
    ) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.clickable {
                    rememberItemGroup.value = !rememberItemGroup.value
                }, verticalAlignment = Alignment.CenterVertically) {
                    CKHeaderText(
                        text = stringResource(
                            R.string.home_group_chat_list_title,
                            chatGroups.value?.size ?: 0
                        ),
                        headerTextType = HeaderTextType.Normal, color = grayscale2
                    )

                    Box(modifier = Modifier.padding(8.sdp())) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center,
                            modifier = Modifier.size(18.sdp())
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(false)
                }
                .size(24.sdp())) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center,
                    modifier = Modifier.size(20.sdp())
                )
            }
        }
        Column {
            AnimatedVisibility(
                visible = rememberItemGroup.value,
                modifier = Modifier.padding(top = 4.sdp()),
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
                    Column {
                        item.forEach { chatGroup ->
                            ChatGroupItemView(
                                modifier = Modifier.padding(start = 15.sdp()),
                                chatGroup = chatGroup,
                                onItemClickListener = onItemClickListener
                            )
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
    val chatGroup = viewModel.directGroups.observeAsState()
    val rememberItemGroup = remember { mutableStateOf(true) }
    Column(modifier = Modifier.wrapContentHeight()) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            rememberItemGroup.value = !rememberItemGroup.value
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    CKHeaderText(
                        text = stringResource(
                            R.string.home_peer_chat_list_title,
                            chatGroup.value?.size ?: 0
                        ),
                        headerTextType = HeaderTextType.Normal, color = grayscale2
                    )
                    Box(modifier = Modifier.padding(8.sdp())) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center,
                            modifier = Modifier.size(20.sdp()),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(true)
                }
                .size(24.sdp())) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center,
                    modifier = Modifier.size(20.sdp()),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
        Column {
            AnimatedVisibility(
                visible = rememberItemGroup.value,
                modifier = Modifier.padding(bottom = 20.sdp()),
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
                val listUserStatus = viewModel.listUserInfo.observeAsState()
                chatGroup.value?.let { item ->
                    Column {
                        item.forEach { chatGroup ->
                            ItemListDirectMessage(
                                modifier = Modifier.padding(start = 16.sdp()),
                                chatGroup,
                                listUserStatus.value,
                                viewModel.getClientIdOfActiveServer(),
                                onItemClickListener
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun JoinServerComposable(
    onJoinServer: (serverUrl: String) -> Unit,
    gotoProfile: () -> Unit,
    isLoading: Boolean
) {
    val rememberServerUrl = remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            CKCircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.sdp(), end = 16.sdp(), top = 20.sdp())
        ) {
            Spacer(modifier = Modifier.size(24.sdp()))
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                CKHeaderText(
                    text = stringResource(R.string.join_server), modifier = Modifier
                        .weight(0.66f), headerTextType = HeaderTextType.Large
                )
                Column(
                    modifier = Modifier.clickable {
                        gotoProfile.invoke()
                    },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hamburger),
                        null, alignment = Alignment.Center, contentScale = ContentScale.FillBounds
                    )
                }
            }

            Spacer(Modifier.height(25.sdp()))
            Text(
                stringResource(R.string.join_server_caption),
                style = MaterialTheme.typography.body2.copy(
                    fontSize = defaultNonScalableTextSize()
                )
            )
            Spacer(modifier = Modifier.size(21.sdp()))
            CKTextInputField(
                stringResource(R.string.server_url),
                rememberServerUrl,
                keyboardType = KeyboardType.Text,
                singleLine = true,
            )
            Spacer(modifier = Modifier.size(14.sdp()))
            CKButton(
                stringResource(R.string.btn_join),
                onClick = {
                    onJoinServer(rememberServerUrl.value)
                },
                enabled = rememberServerUrl.value.isNotBlank() && !isLoading
            )
            Spacer(modifier = Modifier.size(9.sdp()))
            Text(
                stringResource(R.string.join_server_tips),
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSecondary,
                    fontSize = defaultNonScalableTextSize()
                )
            )
        }
    }
}


@Composable
fun WorkSpaceView(
    homeViewModel: HomeViewModel,
    gotoSearch: () -> Unit,
    createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)?,
    gotoProfile: () -> Unit,
    onNavigateNotes: () -> Unit
) {
    val activeServer = homeViewModel.currentServer.observeAsState()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.sdp(), end = 16.sdp(), top = 20.sdp())
    ) {
        Spacer(modifier = Modifier.size(24.sdp()))
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                text = activeServer.value?.serverName ?: "", modifier = Modifier
                    .weight(0.66f), headerTextType = HeaderTextType.Large
            )
            Column(
                modifier = Modifier.clickable { gotoProfile.invoke() },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hamburger),
                    null, alignment = Alignment.Center,
                    modifier = Modifier.size(24.sdp()),
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        Spacer(Modifier.height(16.sdp()))
        Row(
            modifier = Modifier
                .clickable { gotoSearch.invoke() }
                .background(grayscale5, MaterialTheme.shapes.large)
                .padding(18.sdp())
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(28.sdp()))
            CKText(
                stringResource(R.string.search),
                style = MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.onSecondary,
                    fontWeight = FontWeight.Normal,
                    fontSize = defaultNonScalableTextSize()
                ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        ) {
            Spacer(modifier = Modifier.size(24.sdp()))
            ChatGroupView(
                homeViewModel, createGroupChat = createGroupChat,
                onItemClickListener = onItemClickListener
            )
            Spacer(modifier = Modifier.size(28.sdp()))
            DirectMessagesView(
                homeViewModel,
                createGroupChat = createGroupChat,
                onItemClickListener = onItemClickListener
            )
        }
    }
}


@Composable
fun NoteView(onNavigateNotes: () -> Unit) {
    Row(
        Modifier.clickable { onNavigateNotes() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.ic_notes),
            contentDescription = null,
            modifier = Modifier.size(20.sdp()),
            contentScale = ContentScale.FillBounds
        )
        CKHeaderText(
            text = stringResource(R.string.notes),
            modifier = Modifier.padding(start = 10.sdp()),
            headerTextType = HeaderTextType.Normal,
            color = grayscale2,
        )
    }
}


@Composable
fun AddWorkspace(mainViewModel: HomeViewModel) {
    val showJoinServer = mainViewModel.selectingJoinServer.observeAsState()

    if (showJoinServer.value == true) {
        Column(
            modifier = Modifier
                .size(42.sdp())
                .background(color = Color.Transparent)
                .border(
                    BorderStroke(1.5.dp, primaryDefault),
                    shape = RoundedCornerShape(4.sdp())
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            AddWorkspaceButton(mainViewModel)
        }
    } else {
        AddWorkspaceButton(mainViewModel)
    }
}


@Composable
fun AddWorkspaceButton(mainViewModel: HomeViewModel) {
    Image(
        painter = painterResource(R.drawable.ic_add_server),
        contentDescription = "",
        alignment = Alignment.Center,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .clickable(
                onClick = { mainViewModel.showJoinServer() }
            )
            .size(42.sdp())
    )
}


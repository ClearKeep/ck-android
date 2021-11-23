package com.clearkeep.presentation.screen.chat.contactsearch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.clearkeep.R
import com.clearkeep.presentation.components.*
import com.clearkeep.presentation.components.base.*
import com.clearkeep.presentation.screen.chat.composes.CircleAvatar
import com.clearkeep.utilities.*
import com.clearkeep.common.utilities.network.Status

@Composable
fun SearchUserScreen(
    searchViewModel: SearchViewModel,
    navigateToPeerChat: (people: com.clearkeep.domain.model.User?) -> Unit,
    navigateToChatGroup: (chatGroup: com.clearkeep.domain.model.ChatGroup) -> Unit,
    onClose: () -> Unit
) {
    val friends = searchViewModel.friends.observeAsState()
    val groups = searchViewModel.groups.observeAsState()
    val messages = searchViewModel.messages.observeAsState()
    val profile = searchViewModel.profile.observeAsState()
    val searchQuery = searchViewModel.searchQuery.observeAsState()
    val server = searchViewModel.currentServer.observeAsState()
    val searchMode = searchViewModel.searchMode.observeAsState()
    val getPeopleResponse = searchViewModel.getPeopleResponse.observeAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 16.sdp())
    ) {
        val query = rememberSaveable { mutableStateOf("") }

        Spacer(Modifier.height(44.sdp()))
        Box(Modifier.fillMaxWidth()) {
            CKHeaderText(
                server.value?.serverName ?: "",
                Modifier.align(Alignment.CenterStart),
                HeaderTextType.Large
            )
            Image(
                painterResource(R.drawable.ic_cross),
                null,
                Modifier
                    .clickable { onClose.invoke() }
                    .align(Alignment.CenterEnd),
                colorFilter = LocalColorMapping.current.closeIconFilter
            )
        }
        Spacer(Modifier.height(18.sdp()))
        CKSearchBox(
            query,
            placeholder = stringResource(R.string.home_search_hint),
            maxChars = MAX_SEARCH_LENGTH,
            focusRequester = focusRequester
        ) {
            if (query.value.isNotBlank()) {
                searchViewModel.search(query.value)
            }
        }
        Spacer(Modifier.height(10.sdp()))
        Row(Modifier.fillMaxWidth()) {
            FilterItem(
                stringResource(R.string.search_mode_all),
                isSelected = searchMode.value == SearchMode.ALL
            ) {
                searchViewModel.setSearchMode(SearchMode.ALL)
            }
            FilterItem(
                stringResource(R.string.search_mode_people),
                isSelected = searchMode.value == SearchMode.PEOPLE
            ) {
                searchViewModel.setSearchMode(SearchMode.PEOPLE)
            }
            FilterItem(
                stringResource(R.string.search_mode_groups),
                isSelected = searchMode.value == SearchMode.GROUPS
            ) {
                searchViewModel.setSearchMode(SearchMode.GROUPS)
            }
            FilterItem(
                stringResource(R.string.search_mode_messsages),
                isSelected = searchMode.value == SearchMode.MESSAGES
            ) {
                searchViewModel.setSearchMode(SearchMode.MESSAGES)
            }
        }

        Spacer(Modifier.height(18.sdp()))

        Box(Modifier.fillMaxSize()) {
            if (searchQuery.value.isNullOrBlank() && query.value.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                }
            } else if (!groups.value.isNullOrEmpty() || !friends.value.isNullOrEmpty() || !messages.value.isNullOrEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                ) {
                    friends.value?.let {
                        if (it.isNotEmpty() && !searchQuery.value.isNullOrEmpty()) {
                            if (searchMode.value == SearchMode.ALL) {
                                item {
                                    CKText(
                                        stringResource(R.string.search_result_people),
                                        color = LocalColorMapping.current.headerTextAlt
                                    )
                                }
                            }
                            itemsIndexed(it) { _, friend ->
                                Spacer(Modifier.height(18.sdp()))
                                PeopleResultItem(user = friend, query = searchQuery.value!!) {
                                    navigateToPeerChat(friend)
                                }
                            }
                        }
                    }
                    groups.value?.let {
                        if (it.isNotEmpty() && !searchQuery.value.isNullOrEmpty()) {
                            if (searchMode.value == SearchMode.ALL) {
                                item {
                                    Spacer(Modifier.height(26.sdp()))
                                    CKText(
                                        stringResource(R.string.search_result_group_chat),
                                        color = LocalColorMapping.current.headerTextAlt
                                    )
                                }
                            }
                            itemsIndexed(it) { _, group ->
                                Spacer(Modifier.height(18.sdp()))
                                GroupResultItem(group.groupName, query = searchQuery.value!!) {
                                    navigateToChatGroup(group)
                                }
                            }
                        }
                    }
                    messages.value?.let {
                        if (it.isNotEmpty() && !searchQuery.value.isNullOrEmpty()) {
                            if (searchMode.value == SearchMode.ALL) {
                                item {
                                    Spacer(Modifier.height(26.sdp()))
                                    CKText(
                                        stringResource(R.string.search_result_messages),
                                        color = LocalColorMapping.current.headerTextAlt
                                    )
                                }
                            }
                            itemsIndexed(it) { _, messageWithUser ->
                                Spacer(Modifier.height(18.sdp()))
                                MessageResultItem(
                                    user = messageWithUser.user ?: com.clearkeep.domain.model.User(
                                        "",
                                        "",
                                        ""
                                    ),
                                    message = messageWithUser.message,
                                    group = messageWithUser.group ?: com.clearkeep.domain.model.ChatGroup(
                                        null,
                                        0L,
                                        "",
                                        "",
                                        "",
                                        "",
                                        0L,
                                        "",
                                        0L,
                                        "",
                                        emptyList(),
                                        false,
                                        "",
                                        "",
                                        null,
                                        0L,
                                        0L,
                                        false
                                    ),
                                    query = searchQuery.value!!
                                ) {
                                    val message = messageWithUser.message
                                    navigateToChatGroup(
                                        com.clearkeep.domain.model.ChatGroup(
                                            null,
                                            message.groupId,
                                            "",
                                            "",
                                            "",
                                            "",
                                            0L,
                                            "",
                                            0L,
                                            "",
                                            emptyList(),
                                            false,
                                            "",
                                            "",
                                            null,
                                            0L,
                                            0L,
                                            false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                CKText(
                    stringResource(R.string.search_empty),
                    Modifier.align(Alignment.Center),
                    LocalColorMapping.current.bodyTextDisabled
                )
            }
        }

        if (getPeopleResponse.value?.status == com.clearkeep.common.utilities.network.Status.ERROR) {
            CKAlertDialog(
                title = stringResource(R.string.network_error_dialog_title),
                text = stringResource(R.string.network_error_dialog_text),
                onDismissButtonClick = {
                    searchViewModel.getPeopleResponse.value = null
                }
            )
        }
    }
}

@Composable
private fun RowScope.FilterItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val isDarkTheme = LocalColorMapping.current.isDarkTheme

    val backgroundModifier = if (LocalColorMapping.current.isDarkTheme) {
        val borderModifier = if (isSelected) Modifier.border(1.sdp(), Color(0xFFF3F3F3), RoundedCornerShape(8.sdp())) else Modifier
        Modifier.background(Brush.horizontalGradient(listOf(
            backgroundGradientStart,
            backgroundGradientEnd,
        )), RoundedCornerShape(8.sdp())).then(borderModifier)
    } else {
        Modifier.background(Color.White, RoundedCornerShape(8.sdp())).border(1.sdp(), Color(0xFFF3F3F3), RoundedCornerShape(8.sdp()))

    }
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.sdp()))
            .then(backgroundModifier)
            .clickable { onClick() }
            .padding(vertical = 4.sdp()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sdp().toNonScalableTextSize(),
            color = if (isDarkTheme) Color(0xFFE0E0E0) else if (isSelected) primaryLight else grayscale2
        )
    }
}

@Composable
fun PeopleResultItem(
    modifier: Modifier = Modifier,
    user: com.clearkeep.domain.model.User,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier.then(Modifier.clickable { onClick() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleAvatar(
            emptyList(),
            user.userName,
            size = 64.sdp(),
            false,
        )
        Column(
            Modifier
                .padding(start = 16.sdp())
                .weight(1.0f, true)
        ) {
            HighlightedSearchText(
                Modifier,
                user.userName,
                query,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalColorMapping.current.descriptionText
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
        }
        Spacer(Modifier.width(16.sdp()))
    }
}

@Composable
fun GroupResultItem(groupName: String, query: String, onClick: () -> Unit) {
    HighlightedSearchText(
        Modifier
            .clickable { onClick() }
            .fillMaxWidth(), groupName, query,
        style = MaterialTheme.typography.body2.copy(
            color = LocalColorMapping.current.descriptionText
        ))
}

@Composable
fun MessageResultItem(
    user: com.clearkeep.domain.model.User,
    message: com.clearkeep.domain.model.Message,
    group: com.clearkeep.domain.model.ChatGroup,
    query: String,
    onClick: () -> Unit
) {
    Row(Modifier.clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatar(
            emptyList(),
            user.userName,
            size = 64.sdp(),
            false,
        )
        Column(
            Modifier
                .padding(start = 16.sdp())
                .weight(1.0f, true)
        ) {
            CKText(
                text = user.userName,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalColorMapping.current.descriptionText
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
            HighlightedSearchText(
                Modifier,
                message.message,
                query,
                style = MaterialTheme.typography.body2.copy(
                    color = LocalColorMapping.current.descriptionText
                ), overflow = TextOverflow.Ellipsis, maxLines = 3
            )
            Row(Modifier.fillMaxWidth()) {
                val groupName = if (group.isGroup()) group.groupName else ""

                CKText(
                    text = getTimeAsString(message.createdTime, includeTime = true),
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.W400,
                        color = LocalColorMapping.current.descriptionText
                    ), overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Spacer(Modifier.width(4.sdp()))
                HighlightedSearchText(
                    Modifier,
                    groupName,
                    query,
                    style = MaterialTheme.typography.body2.copy(
                        color = LocalColorMapping.current.descriptionText
                    ), overflow = TextOverflow.Ellipsis, maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(16.sdp()))
    }
}

@Composable
fun HighlightedSearchText(
    modifier: Modifier = Modifier,
    fullString: String,
    query: String,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    val annotatedString = buildAnnotatedString {
        var matchIndex = 0

        while (matchIndex >= 0) {
            val startIndex = if (matchIndex == 0) 0 else matchIndex + 1
            val newIndex = fullString.indexOf(query, startIndex, true)
            if (newIndex >= 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.W400)) {
                    append(fullString.substring(matchIndex until newIndex))
                }
                withStyle(SpanStyle(color = LocalColorMapping.current.profileText)) {
                    append(fullString.substring(newIndex until newIndex + query.length))
                }
                matchIndex = newIndex + query.length
            } else {
                withStyle(SpanStyle(fontWeight = FontWeight.W400)) {
                    val startIndex = if (matchIndex == -1) 0 else matchIndex
                    append(fullString.substring(startIndex until fullString.length))
                }
                break
            }
        }
    }

    CKText(
        annotatedString,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        modifier = modifier
    )
}

private const val MAX_SEARCH_LENGTH = 200
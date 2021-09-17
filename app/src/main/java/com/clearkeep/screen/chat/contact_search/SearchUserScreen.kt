package com.clearkeep.screen.chat.contact_search

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.utilities.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SearchUserScreen(
    searchViewModel: SearchViewModel,
    navigateToPeerChat: (people: User?) -> Unit,
    navigateToChatGroup: (chatGroup: ChatGroup) -> Unit,
    onClose: () -> Unit
) {
    val friends = searchViewModel.friends.observeAsState()
    val groups = searchViewModel.groups.observeAsState()
    val messages = searchViewModel.messages.observeAsState()
    val profile = searchViewModel.profile.observeAsState()
    val searchQuery = searchViewModel.searchQuery.observeAsState()
    val server = searchViewModel.currentServer.observeAsState()
    val searchMode = searchViewModel.searchMode.observeAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    printlnCK("Groups passed to composable ${groups.value} is null or empty? ${groups.value.isNullOrEmpty()}")
    printlnCK("Messages passed to composable ${messages.value}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(grayscaleBackground)
            .padding(horizontal = 16.sdp())
    ) {
        val query = remember { mutableStateOf("") }

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
                    .align(Alignment.CenterEnd))
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
            FilterItem("All", isSelected = searchMode.value == SearchMode.ALL) {
                searchViewModel.setSearchMode(SearchMode.ALL)
            }
            FilterItem("People", isSelected = searchMode.value == SearchMode.PEOPLE) {
                searchViewModel.setSearchMode(SearchMode.PEOPLE)
            }
            FilterItem("Groups", isSelected = searchMode.value == SearchMode.GROUPS) {
                searchViewModel.setSearchMode(SearchMode.GROUPS)
            }
            FilterItem("Messages", isSelected = searchMode.value == SearchMode.MESSAGES) {
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
                                    CKText("PEOPLE", color = grayscale1)
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
                                    CKText("GROUP CHAT", color = grayscale1)
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
                                    CKText("MESSAGES", color = grayscale1)
                                }
                            }
                            itemsIndexed(it) { _, messageWithUser ->
                                Spacer(Modifier.height(18.sdp()))
                                MessageResultItem(
                                    user = messageWithUser.user ?: User("", "", ""),
                                    message = messageWithUser.message,
                                    group = messageWithUser.group ?: ChatGroup(
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
                                        0L
                                    ),
                                    query = searchQuery.value!!
                                ) {
                                    val message = messageWithUser.message
                                        navigateToChatGroup(
                                            ChatGroup(
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
                                                0L
                                            )
                                        )
                                }
                            }
                        }
                    }
                }
            } else {
                CKText(
                    "There is no result for your search",
                    Modifier.align(Alignment.Center),
                    grayscale3
                )
            }
        }
    }
}

@Composable
private fun RowScope.FilterItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.sdp()))
            .background(Color.White, RoundedCornerShape(8.sdp()))
            .border(1.sdp(), Color(0xFFF3F3F3), RoundedCornerShape(8.sdp()))
            .clickable { onClick() }
            .padding(vertical = 4.sdp()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sdp().toNonScalableTextSize(),
            color = if (isSelected) primaryLight else grayscale2
        )
    }
}

@Composable
fun PeopleResultItem(
    modifier: Modifier = Modifier,
    user: User,
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
                    color = MaterialTheme.colors.onBackground
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
            color = grayscale2
        ))
}

@Composable
fun MessageResultItem(user: User, message: Message, group: ChatGroup, query: String, onClick: () -> Unit) {
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
                    color = grayscale2
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
            HighlightedSearchText(
                Modifier,
                message.message,
                query,
                style = MaterialTheme.typography.body2.copy(
                    color = grayscale2
                ), overflow = TextOverflow.Ellipsis, maxLines = 3
            )
            Row(Modifier.fillMaxWidth()) {
                val groupName = if (group.isGroup()) group.groupName else ""

                CKText(
                    text = getTimeAsString(message.createdTime),
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.W400,
                        color = grayscale2
                    ), overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Spacer(Modifier.width(4.sdp()))
                HighlightedSearchText(
                    Modifier,
                    groupName,
                    query,
                    style = MaterialTheme.typography.body2.copy(
                        color = grayscale2
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
        printlnCK("================= GroupResultItem groupName $fullString")

        while (matchIndex >= 0) {
            printlnCK("GroupResultItem matchIndex $matchIndex")
            val startIndex = if (matchIndex == 0) 0 else matchIndex + 1
            val newIndex = fullString.indexOf(query, startIndex, true)
            printlnCK("GroupResultItem newIndex $newIndex")
            if (newIndex >= 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.W400)) {
                    printlnCK("GroupResultItem normal string ${fullString.substring(matchIndex until newIndex)}")
                    append(fullString.substring(matchIndex until newIndex))
                }
                withStyle(SpanStyle(color = Color.Black)) {
                    printlnCK("GroupResultItem keyword string $query")
                    printlnCK("GroupResultItem added keyword string ${fullString.substring(newIndex until newIndex + query.length)}")
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
package com.clearkeep.screen.chat.contact_search

import android.content.ClipData
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleBackground
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.composes.NewFriendListItem
import com.clearkeep.utilities.sdp
import com.clearkeep.screen.chat.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.profile.ProfileViewModel
import com.clearkeep.utilities.printlnCK
import java.util.regex.Matcher

@Composable
fun SearchUserScreen(
    searchViewModel: SearchViewModel,
    onFinish: (people: User?) -> Unit
) {
    val friends = searchViewModel.friends.observeAsState()

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
                "CK Development",
                Modifier.align(Alignment.CenterStart),
                HeaderTextType.Large
            )
            Image(
                painterResource(R.drawable.ic_cross),
                null,
                Modifier
                    .clickable { onFinish.invoke(null) }
                    .align(Alignment.CenterEnd))
        }
        Spacer(Modifier.height(18.sdp()))
        CKSearchBox(
            query,
            placeholder = stringResource(R.string.home_search_hint),
            maxChars = MAX_SEARCH_LENGTH
        ) {
            if (query.value.isNotBlank()) {
                searchViewModel.search(query.value)
            }
        }

        Spacer(Modifier.height(18.sdp()))

        Box(Modifier.fillMaxSize()) {
            friends.value?.let {
                if (query.value.isEmpty()) {
                    Column(Modifier.fillMaxSize()) {
                        CKText("Search options")
                        Spacer(Modifier.height(8.sdp()))

                        Column(
                            Modifier
                                .background(Color.White, RoundedCornerShape(8.sdp()))
                                .clip(RoundedCornerShape(8.sdp()))
                        ) {
                            SearchOptionsItem("to", query = "people")
                            SearchOptionsItem("from", query = "people")
                            SearchOptionsItem("in", query = "group chats")
                        }
                    }
                } else if (it.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                    ) {
                        item {
                            CKText("PEOPLE", color = grayscale1)
                        }
                        itemsIndexed(it) { _, friend ->
                            Spacer(Modifier.height(18.sdp()))
                            PeopleResultItem(user = friend)
                        }
                        item {
                            Spacer(Modifier.height(26.sdp()))
                            CKText("GROUP CHAT", color = grayscale1)
                        }
                        itemsIndexed(it) { _, friend ->
                            Spacer(Modifier.height(18.sdp()))
                            GroupResultItem("CK Development", query = "CK")
                        }
                        item {
                            Spacer(Modifier.height(26.sdp()))
                            CKText("MESSAGES", color = grayscale1)
                        }
                        itemsIndexed(it) { _, friend ->
                            Spacer(Modifier.height(18.sdp()))
                            MessageResultItem(user = friend, message = "Hello world!")
                        }
                    }
                } else {
                    Text(
                        "There is no result for your search",
                        Modifier.align(Alignment.Center),
                        grayscale3
                    )
                }
            }
        }
    }
}

@Composable
fun SearchOptionsItem(keyword: String, query: String) {
    Column(
        Modifier
            .background(Color.White)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.sdp(), horizontal = 16.sdp())
        ) {
            CKText("$keyword: ", fontWeight = FontWeight.Bold)
            CKText(query)
        }
        Divider(Modifier.fillMaxWidth(), color = colorResource(R.color.line))
    }
}

@Composable
fun PeopleResultItem(modifier: Modifier = Modifier, user: User) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
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
                    color = MaterialTheme.colors.onBackground
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
        }
        Spacer(Modifier.width(16.sdp()))
    }
}

@Composable
fun GroupResultItem(groupName: String, query: String) {
    val annotatedString = buildAnnotatedString {
        var matchIndex = 0
        printlnCK("================= GroupResultItem groupName $groupName")

        while (matchIndex >= 0) {
            printlnCK("GroupResultItem matchIndex $matchIndex")
            val startIndex = if (matchIndex == 0) 0 else matchIndex + 1
            val newIndex = groupName.indexOf(query, startIndex, true)
            printlnCK("GroupResultItem newIndex $newIndex")
            if (newIndex >= 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.W400)) {
                    printlnCK("GroupResultItem normal string ${groupName.substring(matchIndex until newIndex)}")
                    append(groupName.substring(matchIndex until newIndex))
                }
                withStyle(SpanStyle(fontWeight = FontWeight.W700)) {
                    printlnCK("GroupResultItem keyword string $query")
                    append(query)
                }
                matchIndex = newIndex + query.length
            } else {
                withStyle(SpanStyle(fontWeight = FontWeight.W400)) {
                    val startIndex = if (matchIndex == -1) 0 else matchIndex
                    append(groupName.substring(startIndex until groupName.length))
                }
                break
            }
        }
    }

    CKText(annotatedString)
}

@Composable
fun MessageResultItem(modifier: Modifier = Modifier, user: User, message: String) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
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
            CKText(
                text = message,
                style = MaterialTheme.typography.body2.copy(
                    color = grayscale2
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
        }
        Spacer(Modifier.width(16.sdp()))
    }
}

@Composable
fun SearchResultItem(
//    friend: User,
//    onFriendSelected: (User) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 16.sdp())
            .clickable {
//                onFriendSelected.invoke(chatGroup.groupId)
            }
    ) {
//        val partnerUser= chatGroup.clientList.firstOrNull { client ->
//            client.userId != clintId
//        }
//        val roomName = partnerUser?.userName ?: ""
//        val userStatus = listUserStatus?.firstOrNull { client ->
//            client.userId == partnerUser?.userId
//        }?.userStatus ?: ""
//        val avatar = listUserStatus?.firstOrNull { client ->
//            client.userId == partnerUser?.userId
//        }?.avatar ?: ""

        Row(
            modifier = Modifier.padding(top = 16.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            CircleAvatarStatus(url = avatar, name = roomName, status = userStatus)
            Text(
                text = "Hello", modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.sdp()),
                maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private const val MAX_SEARCH_LENGTH = 200
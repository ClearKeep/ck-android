package com.clearkeep.presentation.screen.chat.contactlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.presentation.components.base.CKDivider
import com.clearkeep.presentation.components.base.CKTopAppBar
import com.clearkeep.domain.model.User
import com.clearkeep.presentation.screen.chat.composes.CircleAvatar
import com.clearkeep.utilities.sdp

@Composable
fun PeopleScreen(
    peopleViewModel: PeopleViewModel,
    onFriendSelected: (com.clearkeep.domain.model.User) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val friends = peopleViewModel.friends.observeAsState()
    Column {
        CKTopAppBar(
            title = {
                Text(text = stringResource(id = R.string.bottom_nav_group))
            },
            actions = {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = ""
                    )
                }
            }
        )
        friends.value?.let { people ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 30.sdp(),
                    end = 20.sdp(),
                    start = 20.sdp(),
                    bottom = 30.sdp()
                ),
            ) {
                itemsIndexed(people) { _, friend ->
                    FriendItem(friend, onFriendSelected)
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: com.clearkeep.domain.model.User,
    onFriendSelected: (com.clearkeep.domain.model.User) -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = { onFriendSelected(friend) }, enabled = true)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(emptyList(), friend.userName)
            Column(
                modifier = Modifier
                    .padding(start = 20.sdp())
                    .fillMaxWidth()
            ) {
                Text(
                    text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.sdp()))
        CKDivider(modifier = Modifier.padding(start = 68.sdp()), thickness = 0.3.dp)
        Spacer(modifier = Modifier.height(10.sdp()))
    }
}
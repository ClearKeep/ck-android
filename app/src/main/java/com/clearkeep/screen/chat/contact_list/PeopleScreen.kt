package com.clearkeep.screen.chat.contact_list

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKDivider
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.composes.CircleAvatar

@Composable
fun PeopleScreen(
    peopleViewModel: PeopleViewModel,
    onFriendSelected: (User) -> Unit,
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
                    contentPadding = PaddingValues(top = 30.dp, end = 20.dp, start = 20.dp, bottom = 30.dp),
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
    friend: User,
    onFriendSelected: (User) -> Unit,
) {
    Column(modifier = Modifier
        .clickable(onClick = { onFriendSelected(friend) }, enabled = true)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(emptyList(), friend.userName)
            Column(modifier = Modifier.padding(start = 20.dp).fillMaxWidth()) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        CKDivider(modifier = Modifier.padding(start = 68.dp), thickness = 0.3.dp)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Preview
@Composable
fun FriendItemPreview() {
    FriendItem(User( "", "test", "")) {}
}
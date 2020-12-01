package com.clearkeep.screen.chat.main.people

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.db.model.People
import com.clearkeep.components.ckDividerColor
import com.clearkeep.screen.chat.main.composes.CircleAvatar

@Composable
fun PeopleScreen(
        peopleViewModel: PeopleViewModel,
        onFriendSelected: (People) -> Unit,
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
                        Icon(Icons.Filled.Search)
                    }
                }
        )
        friends?.let {
            LazyColumnFor(
                    modifier = Modifier.fillMaxSize(),
                    items = it?.value?.data ?: emptyList(),
                    contentPadding = PaddingValues(top = 30.dp, end = 20.dp, start = 20.dp, bottom = 30.dp),
            ) { friend ->
                Surface(color = Color.White) {
                    FriendItem(friend, onFriendSelected)
                }
            }
        }
    }
}

@Composable
fun FriendItem(
        friend: People,
        onFriendSelected: (People) -> Unit,
) {
    Column(modifier = Modifier
        .clickable(onClick = { onFriendSelected(friend) }, enabled = true)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar("")
            Column(modifier = Modifier.padding(start = 20.dp).fillMaxWidth()) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = ckDividerColor, thickness = 0.3.dp, modifier = Modifier.padding(start = 68.dp))
        Spacer(modifier = Modifier.height(10.dp))
    }
}
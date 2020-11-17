package com.clearkeep.screen.chat.main.people

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearkeep.db.model.People
import com.clearkeep.components.ckDividerColor

@Composable
fun PeopleScreen(
        peopleViewModel: PeopleViewModel,
        onFriendSelected: (People) -> Unit,
) {
    val friends = peopleViewModel.friends.observeAsState()
    Column {
        TopAppBar(
                title = {
                    Text(text = "Friends")
                },
        )
        friends?.let {
            LazyColumnFor(
                    items = it?.value?.data ?: emptyList(),
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row() {
            Text(text = friend.userName,
                    style = MaterialTheme.typography.h6
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = ckDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
    }
}
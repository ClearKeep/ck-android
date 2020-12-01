package com.clearkeep.screen.chat.search

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.components.ckDividerColor
import com.clearkeep.db.model.People
import com.clearkeep.screen.chat.main.composes.CircleAvatar
import com.clearkeep.screen.chat.main.composes.InputSearchBox

@Composable
fun SearchUserScreen(
    searchViewModel: SearchViewModel,
    onFinish: (people: People?) -> Unit
) {
    val friends = searchViewModel.friends.observeAsState()
    Column(
            modifier = Modifier.fillMaxSize()
    ) {
        CKTopAppBar(
                title = {
                    InputSearchBox(onValueChange = { text ->
                        searchViewModel.search(text)
                    })
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                onFinish(null)
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        friends?.value?.let {
            LazyColumnFor(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                    items = it,
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp, start = 20.dp, bottom = 20.dp),
            ) { friend ->
                Surface(color = Color.White) {
                    FriendItem(friend, onFinish)
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
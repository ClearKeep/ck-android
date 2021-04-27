package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale2
import com.clearkeep.db.clear_keep.model.People

@Composable
fun FriendListItem(
    friend: People,
    onFriendSelected: ((people: People) -> Unit)?= null,
) {
    Column(modifier = Modifier
        .clickable {
            if (onFriendSelected != null) {
                onFriendSelected(friend)
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName,
                size = 64.dp
            )
            Column(modifier = Modifier
                .padding(start = 16.dp)
                .weight(1.0f, true)) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = grayscale2
                    ),
                )
            }
        }
    }
}
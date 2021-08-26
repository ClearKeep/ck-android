package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKRadioButton
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.utilities.sdp

@Composable
fun FriendListItemSelectable(
    friend: User,
    isSelected: Boolean,
    onFriendSelected: (people: User, isAdd: Boolean) -> Unit,
) {
    Column(modifier = Modifier
        .selectable(
            selected = isSelected,
            onClick = { onFriendSelected(friend, !isSelected) })
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName,
                size = 64.sdp()
            )
            Column(modifier = Modifier
                .padding(start = 16.sdp())
                .weight(1.0f, true)) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    ),
                )
            }
            CKRadioButton(
                selected = isSelected,
                onClick = { onFriendSelected(friend, !isSelected) }
            )
        }
    }
}
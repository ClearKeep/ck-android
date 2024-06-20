package com.clearkeep.features.chat.presentation.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.CKRadioButton
import com.clearkeep.domain.model.User
import com.clearkeep.common.utilities.sdp

@Composable
fun FriendListItemSelectable(
    modifier: Modifier = Modifier,
    friend: User,
    isSelected: Boolean,
    onFriendSelected: (people: User, isAdd: Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .selectable(
                selected = isSelected,
                onClick = { onFriendSelected(friend, !isSelected) })
            .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                listOf(friend.avatar.orEmpty()),
                friend.userName,
                size = 64.sdp()
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.sdp())
                    .weight(1.0f, true)
            ) {
                Text(
                    text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = LocalColorMapping.current.descriptionText
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
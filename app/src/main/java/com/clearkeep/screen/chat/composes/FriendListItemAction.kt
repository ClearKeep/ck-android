package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.colorSuccessDefault
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.utilities.sdp

@Composable
fun FriendListItemAction(
    actionLabel: String,
    friend: User,
    onAction: (profile: User) -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable { onAction(friend) }
            .padding(vertical = 8.sdp())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName ?: "",
                size = 64.sdp()
            )
            Column(Modifier
                .padding(start = 16.sdp())
                .weight(1.0f, true)
            ) {
                Text(
                    text = friend.userName ?: "",
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    ), overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Text("Online", color = colorSuccessDefault)
            }
            Spacer(Modifier.width(16.sdp()))
            CKButton(
                actionLabel,
                {},
                Modifier.width(123.sdp()),
                buttonType = ButtonType.BorderGradient
            )
        }
    }
}
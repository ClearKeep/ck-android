package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.db.clear_keep.model.User

@Composable
fun FriendListItem(
    friend: User,
    onFriendSelected: ((people: User) -> Unit)? = null,
) {
    Column(modifier = Modifier
        .clickable {
            if (onFriendSelected != null) {
                onFriendSelected(friend)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName,
                size = 64.dp
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1.0f, true)
            ) {
                Text(
                    text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    ),
                )
            }
        }
    }
}

@Composable
fun FriendListItemInfo(
    friend: User,
    onFriendSelected: ((people: User) -> Unit)? = null,
    paddingX: Int
) {
    Column(
        modifier = Modifier
            .clickable {
                if (onFriendSelected != null) {
                    onFriendSelected(friend)
                }
            }
            .padding(start = paddingX.dp)
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName,
                size = 36.dp
            )
        }
    }
}

@Composable
fun FriendListMoreItem(count: Int, paddingX: Int) {
    Row() {
        Spacer(Modifier.width(paddingX.dp))
        Box(
            Modifier
                .size(36.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    ), CircleShape
                )
        ) {
            Text("+$count", color = grayscaleOffWhite, fontSize = 14.sp, fontWeight = FontWeight.W700, modifier = Modifier.align(Alignment.Center))
        }
    }
}
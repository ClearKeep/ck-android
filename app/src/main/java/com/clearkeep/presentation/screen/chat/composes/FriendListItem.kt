package com.clearkeep.presentation.screen.chat.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.backgroundGradientEnd
import com.clearkeep.presentation.components.backgroundGradientStart
import com.clearkeep.presentation.components.grayscaleOffWhite
import com.clearkeep.domain.model.User
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

@Composable
fun FriendListItem(
    modifier: Modifier = Modifier,
    friend: User,
    onFriendSelected: ((people: User) -> Unit)? = null,
) {
    Column(modifier = Modifier
        .clickable {
            if (onFriendSelected != null) {
                onFriendSelected(friend)
            }
        }
        .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
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
                        color = LocalColorMapping.current.descriptionText,
                        fontSize = defaultNonScalableTextSize()
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
            .padding(start = paddingX.sdp())
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                arrayListOf(friend.avatar ?: ""),
                friend.userName,
                size = 36.sdp()
            )
        }
    }
}

@Composable
fun FriendListMoreItem(count: Int, paddingX: Int) {
    Row() {
        Spacer(Modifier.width(paddingX.sdp()))
        Box(
            Modifier
                .size(36.sdp())
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    ), CircleShape
                )
        ) {
            Text(
                "+$count",
                color = grayscaleOffWhite,
                fontSize = defaultNonScalableTextSize(),
                fontWeight = FontWeight.W700,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
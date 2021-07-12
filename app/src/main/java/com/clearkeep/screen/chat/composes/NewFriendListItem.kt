package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.User

@Composable
fun NewFriendListItem(
    modifier: Modifier = Modifier,
    user: User,
    description: @Composable ColumnScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    statusIcon: @Composable (BoxScope.() -> Unit) = { StatusIndicator() }
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarNew(emptyList(), user.userName, statusIcon = statusIcon)
        Column(
            Modifier
                .padding(start = 16.dp)
                .weight(1.0f, true)
        ) {
            Text(
                text = user.userName,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
            description()
        }
        Spacer(Modifier.width(16.dp))
        action()
    }
}

@Composable
fun BannedUserItem(modifier: Modifier = Modifier, user: User, onAction: (user: User) -> Unit) {
    NewFriendListItem(modifier,
        user,
        { StatusText(user) },
        {
            CKButton(
                "Unbanned",
                { onAction(user) },
                Modifier.width(123.dp),
                buttonType = ButtonType.BorderGradient
            )
        }
    )
}

@Composable
fun BoxScope.StatusIndicator() {
    Box(
        Modifier
            .size(16.dp)
            .background(colorSuccessDefault, CircleShape)
            .align(Alignment.BottomEnd)
    )

}

@Composable
fun InviteFromFacebookItem(
    modifier: Modifier,
    user: User,
    isSelected: Boolean,
    onFriendSelected: (people: User, isAdd: Boolean) -> Unit,
) {
    NewFriendListItem(modifier,
        user,
        { Text("Facebook Friend", color = Color(0xFF3F65EC)) },
        {
            CKRadioButton(
                isSelected,
                { onFriendSelected(user, !isSelected) },
            )
        }, {
            Image(painterResource(R.drawable.ic_icons_facebook), null,
                Modifier
                    .background(Color.White, CircleShape)
                    .align(Alignment.BottomEnd))
        }
    )
}

@Composable
fun GroupMemberItem(modifier: Modifier, user: User) {
    NewFriendListItem(modifier, user, { StatusText(user) })
}

@Composable
fun CircleAvatarNew(
    url: List<String>,
    name: String = "",
    size: Dp = 64.dp,
    isGroup: Boolean = false,
    statusIcon: @Composable BoxScope.() -> Unit
) {
    if (!url.isNullOrEmpty()) {
        //
    }

    if (isGroup) {
        Image(
            imageVector = Icons.Filled.Groups,
            contentDescription = "",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    color = colorTest
                )
                .padding(12.dp),
        )
    } else {
        val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
        Box(
            modifier = Modifier
                .size(size)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                backgroundGradientStart,
                                backgroundGradientEnd
                            )
                        )
                    )
                    .size(size),
            ) {
                Text(
                    displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.onSurface,
                    ), modifier = Modifier.align(Alignment.Center)
                )
            }
            statusIcon()
        }
    }
}

@Composable
fun StatusText(user: User) {
    Text("Online", color = colorSuccessDefault)
}

@Composable
@Preview
fun NewFriendListItemPreview() {
    val user = User( "", "Alex Mendes", "")

    Column {
        BannedUserItem(Modifier, user) {

        }

        GroupMemberItem(Modifier, user)

        InviteFromFacebookItem(Modifier, user, true) { people: User, isAdd: Boolean ->

        }
    }
}

@Composable
@Preview
fun CircleAvatarNewPreview() {
    Box(Modifier.background(Color.Black)) {
        CircleAvatarNew(emptyList(), "Linh Nguyen", statusIcon = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(colorSuccessDefault, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        })
    }
}
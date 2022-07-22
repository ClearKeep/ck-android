package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.components.LocalColorMapping
import com.clearkeep.components.base.*
import com.clearkeep.components.colorSuccessDefault
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.screen.chat.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.home.composes.StatusIndicator
import com.clearkeep.utilities.sdp

@Composable
fun NewFriendListItem(
    modifier: Modifier = Modifier,
    user: User,
    description: @Composable ColumnScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    statusIcon: @Composable (BoxScope.() -> Unit) = { StatusIndicator(colorSuccessDefault) }
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarStatus(
            user.avatar,
            user.userName,
            size = 64.sdp(),
            user.userStatus ?: UserStatus.ONLINE.value,
            16.sdp()
        )
        Column(
            Modifier
                .padding(start = 16.sdp())
                .weight(1.0f, true)
        ) {
            CKText(
                text = user.userName,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalColorMapping.current.inputLabel
                ), overflow = TextOverflow.Ellipsis, maxLines = 1
            )
            description()
        }
        Spacer(Modifier.width(16.sdp()))
        action()
    }
}

@Composable
fun StatusText(user: User) {
    //CKText("Online", color = colorSuccessDefault)
}
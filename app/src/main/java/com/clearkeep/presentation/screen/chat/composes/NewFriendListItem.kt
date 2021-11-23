package com.clearkeep.presentation.screen.chat.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.*
import com.clearkeep.presentation.components.colorSuccessDefault
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserStatus
import com.clearkeep.presentation.screen.chat.home.composes.CircleAvatarStatus
import com.clearkeep.presentation.screen.chat.home.composes.StatusIndicator
import com.clearkeep.utilities.sdp

@Composable
fun NewFriendListItem(
    modifier: Modifier = Modifier,
    user: com.clearkeep.domain.model.User,
    description: @Composable ColumnScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    statusIcon: @Composable (BoxScope.() -> Unit) = { StatusIndicator(colorSuccessDefault) }
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarStatus(
            user.avatar,
            user.userName,
            size = 64.sdp(),
            user.userStatus ?: com.clearkeep.domain.model.UserStatus.ONLINE.value,
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
fun StatusText(user: com.clearkeep.domain.model.User) {
    //CKText("Online", color = colorSuccessDefault)
}
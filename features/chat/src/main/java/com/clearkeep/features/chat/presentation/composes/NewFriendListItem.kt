package com.clearkeep.features.chat.presentation.composes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.common.presentation.components.colorSuccessDefault
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserStatus
import com.clearkeep.common.utilities.sdp
import com.clearkeep.features.chat.presentation.home.composes.CircleAvatarStatus
import com.clearkeep.features.chat.presentation.home.composes.StatusIndicator

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
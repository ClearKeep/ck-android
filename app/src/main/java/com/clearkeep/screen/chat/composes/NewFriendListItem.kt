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
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.screen.chat.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.home.composes.StatusIndicator

@Composable
fun NewFriendListItem(
    modifier: Modifier = Modifier,
    user: User,
    description: @Composable ColumnScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    statusIcon: @Composable (BoxScope.() -> Unit) = { StatusIndicator(colorSuccessDefault) }
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleAvatarStatus(null, user.userName, size = 64.dp,user.userStatus?:UserStatus.ONLINE.value,16.dp)
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
fun StatusText(user: User) {
    Text("Online", color = colorSuccessDefault)
}
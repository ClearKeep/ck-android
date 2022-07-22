package com.clearkeep.screen.chat.group_invite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.utilities.sdp

@Composable
fun SelectedFriendBox(
    people: User,
    onRemove: (User) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (LocalColorMapping.current.isDarkTheme) primaryDefault else grayscale5,
        elevation = 0.sdp(),
        modifier = Modifier.clickable { onRemove(people) }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 5.sdp(), horizontal = 8.sdp()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                people.userName, style = MaterialTheme.typography.caption.copy(
                    color = LocalColorMapping.current.bodyTextAlt
                )
            )
            Spacer(modifier = Modifier.width(13.sdp()))
            Icon(
                Icons.Filled.Close,
                contentDescription = "",
                modifier = Modifier.size(12.sdp()),
                tint = LocalColorMapping.current.iconColorAlt
            )
        }
    }
}
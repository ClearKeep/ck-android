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
import com.clearkeep.components.grayscale1
import com.clearkeep.components.grayscale5
import com.clearkeep.components.grayscaleBlack
import com.clearkeep.db.clear_keep.model.People

@Composable
fun SelectedFriendBox(
    people: People,
    onRemove: (People) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = grayscale5,
        elevation = 0.dp,
        modifier = Modifier.clickable { onRemove(people) }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(people.userName, style = MaterialTheme.typography.caption.copy(
                color = grayscaleBlack
            ))
            Spacer(modifier = Modifier.width(13.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "",
                modifier = Modifier.size(12.dp),
                tint = grayscale1
            )
        }
    }
}
package com.clearkeep.screen.chat.main.home.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.Server


@Composable
fun CircleAvatarWorkSpace(item: Server, idWorkSpaceLive: Long?) {
    val displayName =
        if (item.serverName.isNotBlank() && item.serverName.length >= 2) item.serverName.substring(0, 2) else item.serverName
    Surface(
        shape = CircleShape,
        modifier = Modifier.size(32.dp)
    ) {
        Column(
            modifier = Modifier.background(
                shape = CircleShape,
                color = primaryDefault
            ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSurface,
                )
            )
        }

    }
}

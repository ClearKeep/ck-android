package com.clearkeep.screen.chat.main.home.composes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.Server


@Composable
fun CircleAvatarWorkSpace(item: Server, isHighlight: Boolean) {
    if (isHighlight) Highlight(item) else Content(item)
}

@Composable
fun Highlight(item: Server) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .background(color = Color.Transparent)
            .border(
                BorderStroke(1.5.dp, primaryDefault),
                shape = CircleShape
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        Content(item)
    }
}

@Composable
fun Content(item: Server) {
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

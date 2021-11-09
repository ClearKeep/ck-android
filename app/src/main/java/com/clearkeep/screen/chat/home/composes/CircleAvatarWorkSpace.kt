package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.LocalColorMapping
import com.clearkeep.components.base.CKText
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.Server


@Composable
fun CircleAvatarWorkSpace(
    item: Server,
    isHighlight: Boolean,
    contentSize: Dp = dimensionResource(R.dimen._32sdp)
) {
    if (isHighlight) Highlight(item, contentSize) else Content(item, contentSize)
}

@Composable
fun Highlight(item: Server, contentSize: Dp) {
    Column(
        modifier = Modifier
            .size(contentSize.plus(dimensionResource(R.dimen._12sdp)))
            .background(color = Color.Transparent)
            .border(
                BorderStroke(dimensionResource(R.dimen._1sdp), if (LocalColorMapping.current.isDarkTheme) Color.Black else primaryDefault),
                shape = RoundedCornerShape(dimensionResource(R.dimen._4sdp))
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        Content(item, contentSize)
    }
}

@Composable
fun Content(item: Server, contentSize: Dp) {
    val displayName =
        if (item.serverName.isNotBlank() && item.serverName.length >= 2) item.serverName.substring(
            0,
            2
        ) else item.serverName
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen._4sdp)),
        modifier = Modifier.size(contentSize)
    ) {
        Column(
            modifier = Modifier.background(
                shape = RoundedCornerShape(dimensionResource(R.dimen._4sdp)),
                color = primaryDefault
            ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CKText(
                displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSurface,
                )
            )
        }

    }
}

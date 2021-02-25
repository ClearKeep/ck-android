package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearkeep.components.colorTest

@Composable
fun CircleAvatar(
        url: String,
        isGroup: Boolean = false,
        size: Dp = 48.dp,
        elevation: Dp = 0.dp,
) {
    Card(modifier = Modifier
        .size(size),
        shape = CircleShape,
        backgroundColor = colorTest,
        contentColor = Color.White,
        elevation = elevation,
    ){
        if (isGroup) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = "",
                modifier = Modifier.size(36.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
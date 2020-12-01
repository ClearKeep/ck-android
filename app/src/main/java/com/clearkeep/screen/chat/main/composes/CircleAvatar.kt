package com.clearkeep.screen.chat.main.composes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.components.colorTest

@Composable
fun CircleAvatar(
        url: String,
        isGroup: Boolean = false,
        size: Dp = 48.dp,
        elevation: Dp = 0.dp,
) {
    Card(modifier = Modifier
        .preferredSize(size),
        shape = CircleShape,
        backgroundColor = colorTest,
        contentColor = Color.White,
        elevation = elevation,
    ){
        if (isGroup) {
            Icon(Icons.Filled.Groups.copy(defaultHeight = 36.dp, defaultWidth = 36.dp))
        } else {
            Icon(Icons.Filled.Person.copy(defaultHeight = 36.dp, defaultWidth = 36.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircleAvatar() {
    CircleAvatar("")
}
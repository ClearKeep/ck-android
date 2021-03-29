package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearkeep.components.colorTest

@Composable
fun CircleAvatar(
        url: List<String>,
        name: String = "",
        size: Dp = 48.dp,
        elevation: Dp = 0.dp,
        isGroup: Boolean = false
) {
    if (!url.isNullOrEmpty()) {
        //
    }

    if (isGroup) {
        Image(
            imageVector = Icons.Filled.Groups,
            contentDescription = "",
            modifier = Modifier.size(size).clip(CircleShape).background(
                color = colorTest
            ).padding(12.dp),
        )
    } else {
        /*val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
        Card(
            shape = CircleShape,
            backgroundColor = colorTest,
            contentColor = Color.White,
            elevation = elevation,
            modifier = Modifier
                .size(size)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(displayName.capitalize())
            }
        }*/
        Image(
            imageVector = Icons.Filled.Person,
            contentDescription = "",
            modifier = Modifier.size(size).clip(CircleShape).background(
                color = colorTest
            ).padding(12.dp),
        )
    }
}
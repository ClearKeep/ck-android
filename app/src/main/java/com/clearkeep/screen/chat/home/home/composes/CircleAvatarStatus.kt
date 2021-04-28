package com.clearkeep.screen.chat.home.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart

@Composable
fun CircleAvatarStatus(url: String?, name: String, size: Dp = 24.dp, status: String) {
    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name

    Column(Modifier.size(size)) {
        Column(
            modifier = Modifier
                .background(
                    shape = CircleShape,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                )
                .size(size),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSurface,
                )
            )
        }
        Box(modifier = Modifier.size(10.dp).background(
            shape = CircleShape,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Red,
                    Color.Red
                )
            )
        ))
    }
}

enum class UserStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    DEFAULT
}
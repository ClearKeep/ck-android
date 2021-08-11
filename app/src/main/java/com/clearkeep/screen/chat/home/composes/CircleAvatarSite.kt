package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.CachePolicy
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.google.accompanist.coil.rememberCoilPainter

@Composable
fun CircleAvatarSite(url: String?, name: String, size: Dp = 56.dp, status: String) {
    val context = LocalContext.current
    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name

    Column(Modifier.size(size)) {
        if (!url.isNullOrEmpty()) {
            Image(
                rememberCoilPainter(
                    request = url,
                    imageLoader = context.imageLoader,
                    previewPlaceholder = R.drawable.ic_cross,
                    requestBuilder = {
                        memoryCachePolicy(CachePolicy.DISABLED)
                    }
                ),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
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
}

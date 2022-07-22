package com.clearkeep.screen.chat.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.rememberImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.colorTest
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp

@Composable
fun CircleAvatar(
    url: List<String>,
    name: String = "",
    size: Dp = 48.sdp(),
    isGroup: Boolean = false,
    modifier: Modifier = Modifier,
    cacheKey: String = ""
) {
    val context = LocalContext.current
    if (isGroup) {
        Image(
            imageVector = Icons.Filled.Groups,
            contentDescription = "",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    color = colorTest
                )
                .padding(12.sdp())
                .then(modifier),
        )
    } else {
        val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
        if (url.isNotEmpty() && url[0].isNotBlank()) {
            Image(
                rememberImagePainter(
                    "${url[0]}?cache=$cacheKey", // Force recomposition when cache key changes
                    imageLoader = context.imageLoader,
                ),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .then(modifier)
            )
        } else {
            Surface(
                shape = CircleShape,
                modifier = Modifier
                    .size(size)
                    .then(modifier)
            ) {
                Column(
                    modifier = Modifier.background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                backgroundGradientStart,
                                backgroundGradientEnd
                            )
                        )
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
    }
}
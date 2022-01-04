package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.memory.MemoryCache
import com.clearkeep.R
import coil.request.CachePolicy
import com.clearkeep.components.*
import com.clearkeep.components.base.CKText
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp

@Composable
fun CircleAvatarStatus(
    url: String?,
    name: String,
    size: Dp = 24.sdp(),
    status: String,
    sizeIndicator: Dp = 8.sdp(),
    cacheKey: String = ""
) {
    val lastUri = rememberSaveable { mutableStateOf<MemoryCache.Key?>(null) }

    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
    val color = when (status) {
        UserStatus.ONLINE.value -> {
            colorSuccessDefault
        }
        UserStatus.OFFLINE.value, UserStatus.UNDEFINED.value -> {
            grayscale3
        }
        else -> {
            errorDefault
        }
    }
    Box(
        modifier = Modifier
            .size(size)
    ) {
        Column(Modifier.size(size)) {
            if (!url.isNullOrEmpty()) {
                val painter = rememberImagePainter(
                    "$url?cache=$cacheKey", //Force reload when cache key changes
                    builder = {
                        placeholderMemoryCacheKey(lastUri.value)
                        crossfade(250)
                    }
                )
                Image(
                    painter,
                    null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
                if  (painter.state is ImagePainter.State.Success) {
                    val memoryCacheKey = (painter.state as ImagePainter.State.Success).metadata.memoryCacheKey
                    lastUri.value = memoryCacheKey
                }
            } else
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
                    CKText(
                        displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onSurface,
                        )
                    )
                }
        }
        StatusIndicator(color, sizeIndicator)
    }

}

@Composable
fun BoxScope.StatusIndicator(color: Color, size: Dp = 8.sdp()) {
    Box(
        Modifier
            .size(size)
            .background(color, CircleShape)
            .align(Alignment.BottomEnd)
    )
}
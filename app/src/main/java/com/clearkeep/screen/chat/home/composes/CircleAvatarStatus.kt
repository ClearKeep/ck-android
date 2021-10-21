package com.clearkeep.screen.chat.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.clearkeep.R
import coil.request.CachePolicy
import com.clearkeep.components.*
import com.clearkeep.components.base.CKText
import com.clearkeep.db.clear_keep.model.UserStatus
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import com.google.accompanist.coil.rememberCoilPainter

@Composable
fun CircleAvatarStatus(url: String?, name: String, size: Dp = 24.sdp(), status: String) {
    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
    printlnCK("CircleAvatarStatus: url $url  _name: $name")
    when(status){
        UserStatus.ONLINE.value->{
            colorSuccessDefault
        }
        UserStatus.OFFLINE.value,UserStatus.UNDEFINED.value ->{
            grayscale3
        }
        else ->{
            errorDefault
        }
    }
    Box(modifier = Modifier
        .size(size)) {
        Column(Modifier.size(size)) {
            if (!url.isNullOrEmpty())
            {
                    printlnCK("CircleAvatarSite url") // Force recomposition when cache key changes
                    Image(
                        rememberCoilPainter(
                            request = "$url", //Force reload when cache key changes
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
            }
            else
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
    }

}

@Composable
fun BoxScope.StatusIndicator(color: Color, size: Dp=8.sdp()) {
    Box(
        Modifier
            .size(size)
            .background(color, CircleShape)
            .align(Alignment.BottomEnd)
    )
}
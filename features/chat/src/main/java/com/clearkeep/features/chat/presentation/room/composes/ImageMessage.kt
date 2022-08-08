package com.clearkeep.features.chat.presentation.room.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.clearkeep.common.presentation.components.grayscaleOffWhite
import com.clearkeep.common.utilities.isTempMessage
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize

@Composable
fun ImageMessageContent(
    modifier: Modifier,
    imageUris: List<String>,
    isQuote: Boolean,
    onClickItem: (uri: String) -> Unit
) {
    if (isQuote) {
        ImageMessageItem(
            Modifier
                .width(242.sdp())
                .height(60.sdp())
                .aspectRatio(1.33f),
            imageUris[0], cropped = true, onClickItem
        )
    } else if (imageUris.size == 1) {
        ImageMessageItem(
            Modifier
                .then(modifier)
                .size(130.sdp()), imageUris[0], onClick = onClickItem
        )
    } else {
        Column(
            Modifier
                .wrapContentSize()
                .padding(12.sdp())
        ) {
            printlnCK("multi item grid")
            Row(Modifier.wrapContentSize()) {
                for (i in 0..minOf(imageUris.size, 1)) {
                    ImageMessageItem(
                        Modifier
                            .size(110.sdp())
                            .padding(4.sdp()), imageUris[i], onClick = onClickItem
                    )
                }
            }
            Row(Modifier.wrapContentSize()) {
                for (i in 2 until imageUris.size) {
                    if (i == 2 || (i == 3 && imageUris.size <= 4)) {
                        ImageMessageItem(
                            Modifier
                                .size(110.sdp())
                                .padding(4.sdp()), imageUris[i], onClick = onClickItem
                        )
                    } else {
                        Box(
                            Modifier
                                .size(110.sdp())
                                .clip(RoundedCornerShape(16.sdp()))
                                .aspectRatio(1f)
                                .padding(4.sdp()),
                            contentAlignment = Alignment.Center
                        ) {
                            ImageMessageItem(Modifier.fillMaxSize(), imageUris[i], onClick = onClickItem)
                            Box(
                                Modifier
                                    .background(Color(0x4D000000), RoundedCornerShape(16.sdp()))
                                    .fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${imageUris.size - 4}",
                                    Modifier
                                        .align(Alignment.Center),
                                    color = grayscaleOffWhite,
                                    textAlign = TextAlign.Center,
                                    fontSize = 28.sdp().toNonScalableTextSize()
                                )
                            }

                        }
                        break
                    }
                }
            }
        }
    }
}

@ExperimentalCoilApi
@Composable
fun ImageMessageItem(
    modifier: Modifier = Modifier,
    uri: String,
    cropped: Boolean = false,
    onClick: (uri: String) -> Unit
) {
    val lastUri = rememberSaveable { mutableStateOf<MemoryCache.Key?>(null) }

    val context = LocalContext.current
    val clickableModifier =
        if (isTempMessage(uri)) Modifier else Modifier.clickable { onClick.invoke(uri) }
    val sizeModifier = if (cropped) Modifier.fillMaxWidth().height(120.sdp()) else Modifier.fillMaxSize().aspectRatio(1f)
    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
        .data(uri)
        .crossfade(250)
        .placeholderMemoryCacheKey(lastUri.value)
        .build())

    Box(modifier) {
        Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .then(sizeModifier)
                .clip(RoundedCornerShape(16.sdp()))
                .then(clickableModifier)
        )
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(Modifier.fillMaxSize())
            }
            is AsyncImagePainter.State.Success -> {
                val memoryCacheKey = (painter.state as AsyncImagePainter.State.Success).result.memoryCacheKey
                lastUri.value = memoryCacheKey
            }
            else -> {
            }
        }
    }
}
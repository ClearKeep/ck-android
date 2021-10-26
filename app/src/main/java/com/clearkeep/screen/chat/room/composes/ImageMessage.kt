package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.clearkeep.R
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.isTempMessage
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.rememberDrawablePainter

@Composable
fun ImageMessageContent(modifier: Modifier, imageUris: List<String>, onClickItem: (uri: String) -> Unit) {
    printlnCK("ImageMessageContent $imageUris")
    if (imageUris.size == 1) {
        ImageMessageItem(
            Modifier
                .then(modifier)
                .size(130.sdp()), imageUris[0], onClickItem
        )
    } else {
        Column(
            Modifier
                .wrapContentSize()
                .padding(12.sdp())) {
            printlnCK("multi item grid")
            Row(Modifier.wrapContentSize()) {
                for (i in 0..minOf(imageUris.size, 1)) {
                    ImageMessageItem(
                        Modifier
                            .size(110.sdp())
                            .padding(4.sdp()), imageUris[i], onClickItem)
                }
            }
            Row(Modifier.wrapContentSize()) {
                for (i in 2 until imageUris.size) {
                    if (i == 2 || (i == 3 && imageUris.size <= 4)) {
                            ImageMessageItem(
                                Modifier
                                    .size(110.sdp())
                                    .padding(4.sdp()), imageUris[i], onClickItem)
                    } else {
                        Box(
                            Modifier
                                .size(110.sdp())
                                .clip(RoundedCornerShape(16.sdp()))
                                .aspectRatio(1f)
                                .padding(4.sdp()),
                            contentAlignment = Alignment.Center
                        ) {
                            ImageMessageItem(Modifier.fillMaxSize(), imageUris[i], onClickItem)
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

@Composable
fun ImageMessageItem(
    modifier: Modifier = Modifier,
    uri: String,
    onClick: (uri: String) -> Unit
) {
    val context = LocalContext.current
    val clickableModifier = if (isTempMessage(uri)) Modifier else Modifier.clickable { onClick.invoke(uri) }
    val painter = rememberCoilPainter(uri, context.imageLoader)
    Box(modifier) {
        Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.sdp()))
                .aspectRatio(1f)
                .then(clickableModifier)
        )
        when (painter.loadState) {
            is ImageLoadState.Loading -> {
                Box(Modifier.fillMaxSize())
            }
            else -> {}
        }
    }
}
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
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.imageloading.rememberDrawablePainter

@Composable
fun ImageMessageContent(modifier: Modifier, imageUris: List<String>, onClickItem: (uri: String) -> Unit) {
    println("ImageMessageContent $imageUris")
    if (imageUris.size == 1) {
        ImageMessageItem(
            Modifier
                .then(modifier)
                .size(130.dp), imageUris[0], onClickItem
        )
    } else {
        Column(Modifier.wrapContentSize().padding(12.dp)) {
            println("multi item grid")
            Row(Modifier.wrapContentSize()) {
                for (i in 0..minOf(imageUris.size, 1)) {
                    ImageMessageItem(
                        Modifier
                            .size(110.dp)
                            .padding(4.dp), imageUris[i], onClickItem)
                }
            }
            Row(Modifier.wrapContentSize()) {
                for (i in 2 until imageUris.size) {
                    if (i == 2 || (i == 3 && imageUris.size <= 4)) {
                            ImageMessageItem(Modifier.size(110.dp).padding(4.dp), imageUris[i], onClickItem)
                    } else {
                        Box(
                            Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .aspectRatio(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ImageMessageItem(Modifier.fillMaxSize(), imageUris[i], onClickItem)
                            Box(
                                Modifier
                                    .background(Color(0x4D000000), RoundedCornerShape(16.dp))
                                    .fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${imageUris.size - 4}",
                                    Modifier
                                        .align(Alignment.Center),
                                    color = grayscaleOffWhite,
                                    textAlign = TextAlign.Center,
                                    fontSize = 28.sp
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
    val clickableModifier = if (isTempMessage(uri)) Modifier else Modifier.clickable { onClick.invoke(uri) }
    val context = LocalContext.current
    Image(
        painter = rememberCoilPainter(uri, context.imageLoader),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = Modifier
            .then(modifier)
            .clip(RoundedCornerShape(16.dp))
            .aspectRatio(1f)
            .then(clickableModifier)
    )
}
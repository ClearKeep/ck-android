package com.clearkeep.screen.chat.room.photo_detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.components.primaryDefault
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.imageloading.rememberDrawablePainter
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun PhotoDetailScreen() {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        val (imageId, imageListId) = createRefs()
        Box(Modifier.fillMaxHeight(0.875f).constrainAs(imageId) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(imageListId.top)
        }) {
            IconButton(onClick = { /*TODO*/ },
                Modifier.align(Alignment.TopStart)) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = Color.White
                )
            }
            Text("You", Modifier.align(Alignment.TopCenter))
            Image(
                painterResource(R.drawable.test), null,
                Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = { /*TODO*/ }, Modifier
                    .align(Alignment.BottomStart)
                    .wrapContentSize()
            ) {
                Icon(
                    Icons.Default.IosShare,
                    null,
                    tint = Color.White
                )
            }
        }
        BottomImageList(
            Modifier
                .height(48.dp)
                .constrainAs(imageListId) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Composable
fun SelectableImageItem(
    modifier: Modifier = Modifier,
//    uri: String,
//    isSelected: Boolean,
//    onSelect: (String, Boolean) -> Unit
) {
    Box(
        modifier.then(
            Modifier
                .clip(RectangleShape)
                .aspectRatio(1f)
                .clickable {
//                    onSelect(uri, !isSelected)
                }
//                .then(if (isSelected) Modifier.border(2.dp, primaryDefault) else Modifier)
        )
    ) {
        Image(
            painterResource(R.drawable.test),
//            rememberGlidePainter(request = uri, previewPlaceholder = R.drawable.ic_cross),
            null,
            contentScale = ContentScale.Crop,
            modifier = modifier.then(Modifier.align(Alignment.Center)),
        )
    }
}

@Composable
fun ImageCarouselItem() {

}

@Composable
fun BottomImageList(modifier: Modifier = Modifier) {
    LazyRow(modifier) {
        items(8) {
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
            SelectableImageItem(Modifier.padding(horizontal = 2.dp))
        }
    }
}

@Composable
@Preview
fun PhotoDetailScreenPreview() {
    PhotoDetailScreen()
}
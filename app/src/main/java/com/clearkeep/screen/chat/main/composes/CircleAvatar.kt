package com.clearkeep.screen.chat.main.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearkeep.R

@Composable
fun CircleAvatar(
        url: String,
        size: Dp = 48.dp,
        elevation: Dp = 2.dp,
) {
    Card(modifier = Modifier
        .preferredSize(size),
        shape = CircleShape,
        elevation = elevation){

        Image(
            imageResource(R.drawable.ic_avatar),
            contentScale =  ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
    }
}
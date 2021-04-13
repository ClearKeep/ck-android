package com.clearkeep.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.clearkeep.R

@Composable
fun SplashScreen(
) {
    val imageLogo = painterResource(R.drawable.ic_logo)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = imageLogo,
            contentDescription = null
        )
    }
}
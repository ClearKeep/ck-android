package com.clearkeep.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.R

@Composable
fun SplashScreen(
) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column () {
            Text(
                text = "Welcome to e2ee app",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = stringResource(R.string.title_app),
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview
@Composable
fun PreviewApp() {
    SplashScreen()
}
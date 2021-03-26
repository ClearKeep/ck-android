package com.clearkeep.screen.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R

@Composable
fun SplashScreen(
) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                text = "Welcome to e2ee app",
                modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onPrimary)
        )
        Text(
                text = stringResource(R.string.title_app),
                style = MaterialTheme.typography.body2.copy(
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
        )
    }
}
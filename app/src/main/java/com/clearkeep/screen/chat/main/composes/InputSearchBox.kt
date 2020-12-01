package com.clearkeep.screen.chat.main.composes

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.clearkeep.components.colorTest

@Composable
fun InputSearchBox(
        onValueChange: (text: String) -> Unit
) {
    val userName = state { "" }
    TextField(
        modifier = Modifier.fillMaxWidth(),
            value = userName.value,
            onValueChange = {
                userName.value = it
                onValueChange(it)
            },
            placeholder = { Text("search...") },
            activeColor = Color.Blue,
            inactiveColor = colorTest,
            textStyle = MaterialTheme.typography.body2.copy(color = Color.Black)
    )
}
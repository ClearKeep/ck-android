package com.clearkeep.screen.chat.contact_search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun InputSearchBox(
        onValueChange: (text: String) -> Unit
) {
    val userName = mutableStateOf("")
    TextField(
        modifier = Modifier.fillMaxWidth(),
            value = userName.value,
            onValueChange = {
                userName.value = it
                onValueChange(it)
            },
            placeholder = { Text("search...") },
            /*activeColor = Color.Blue,
            inactiveColor = colorTest,*/
            textStyle = MaterialTheme.typography.body2.copy(color = Color.Black)
    )
}
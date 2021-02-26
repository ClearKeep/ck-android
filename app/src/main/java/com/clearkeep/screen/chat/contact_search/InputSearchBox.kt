package com.clearkeep.screen.chat.contact_search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.clearkeep.components.colorTest

@Composable
fun InputSearchBox(
    onValueChange: (text: String) -> Unit
) {
    val userName = remember {mutableStateOf("")}
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = userName.value,
        onValueChange = {
            userName.value = it
            onValueChange(it)
        },
        placeholder = { Text("search...") },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            cursorColor = Color.Blue,
            focusedBorderColor = Color.Blue,
            unfocusedBorderColor = Color.Gray,
        ),
        textStyle = MaterialTheme.typography.body2.copy(color = Color.Black)
    )
}
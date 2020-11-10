package com.clearkeep.ui.base

import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Composable
fun CKTextField(
        label: String,
        placeholder: String,
        textValue: MutableState<String>,
        modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
            modifier = modifier,
            value = textValue.value,
            onValueChange = { textValue.value = it },
            label = {
                Text(label, style = MaterialTheme.typography.body2.copy(
                        color = Color.Gray
                ))
            },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.body2.copy(
                    color = Color.Gray))
            },
            activeColor = Color.Red,
            inactiveColor = Color.Blue,
            textStyle = MaterialTheme.typography.body2.copy(
                    color = Color.Blue
            )
    )
}
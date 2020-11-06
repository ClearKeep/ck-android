package com.clearkeep.ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Layout
import androidx.compose.ui.Measurable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
fun FilledTextInputComponent(
    lable: String,
    placeholder: String,
    textValue: MutableState<String>
) {
    TextField(
        value = textValue.value,
        onValueChange = { textValue.value = it },
        label = { Text(lable) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth(),
        activeColor = Color.Gray
    )
}
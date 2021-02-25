package com.clearkeep.components.base

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.clearkeep.utilities.printlnCK

@Composable
fun CKTextField(
        label: String,
        placeholder: String,
        textValue: MutableState<String>,
        modifier: Modifier = Modifier,
        keyboardType: KeyboardType = KeyboardType.Text,
        passwordVisibility: Boolean = false,
        error: String? = null
) {
        printlnCK("text = ${textValue.value}")
    Column {
            OutlinedTextField(
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
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.Red,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Blue,
                    ),
                    textStyle = MaterialTheme.typography.body2.copy(
                            color = Color.Blue
                    ),
                    keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType
                    ),
                    visualTransformation = if (!passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = modifier.fillMaxWidth(),
            )
            if (!error.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
            }
            if (!error.isNullOrEmpty()) Text(error,
                    style = MaterialTheme.typography.body2.copy(color = Color.Red),
                    modifier = Modifier.padding(start = 8.dp)
            )
    }
}
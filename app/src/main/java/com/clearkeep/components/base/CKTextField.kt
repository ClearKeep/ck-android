package com.clearkeep.components.base

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CKTextField(
    label: String,
    placeholder: String,
    textValue: MutableState<String>,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    val isPasswordType = keyboardType == KeyboardType.Password
    var passwordVisibility = remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = textValue.value,
            onValueChange = { textValue.value = it },
            label = {
                Text(
                    label, style = MaterialTheme.typography.body2.copy(
                        color = Color.Gray
                    )
                )
            },
            placeholder = {
                Text(
                    placeholder, style = MaterialTheme.typography.body2.copy(
                        color = Color.Gray
                    )
                )
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
            visualTransformation = if (isPasswordType) {
                if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            modifier = modifier.fillMaxWidth(),
            trailingIcon = {
                if (isPasswordType) {
                    Icon(
                        imageVector = if (!passwordVisibility.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "",
                        tint = Color.Gray,
                        modifier = Modifier.clickable(
                            onClick = { passwordVisibility.value = !passwordVisibility.value }
                        )
                    )
                }
            },
        )
        if (!error.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(5.dp))
        }
        if (!error.isNullOrEmpty()) Text(
            error,
            style = MaterialTheme.typography.body2.copy(color = Color.Red),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
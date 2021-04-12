package com.clearkeep.components.base

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CKTextInputField(
    placeholder: String,
    textValue: MutableState<String>,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val isPasswordType = keyboardType == KeyboardType.Password
    var passwordVisibility = remember { mutableStateOf(false) }
    Column {
        TextField(
            value = textValue.value,
            onValueChange = { textValue.value = it },
            /*label = {
                if (label.isNotBlank()) {
                    Text(
                        label, style = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.secondaryVariant
                        )
                    )
                }
            },*/
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        placeholder, style = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.secondaryVariant
                        )
                    )
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.secondaryVariant,
                cursorColor = MaterialTheme.colors.secondaryVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                backgroundColor = MaterialTheme.colors.secondary
            ),
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.secondaryVariant
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
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = imeAction, keyboardType = keyboardType),
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
package com.clearkeep.components.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.*

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
    val shape = MaterialTheme.shapes.large
    val isError = !error.isNullOrBlank()
    val focusManager = LocalFocusManager.current

    val isPasswordType = keyboardType == KeyboardType.Password
    var passwordVisibility = remember { mutableStateOf(false) }

    var rememberBorderShow = remember { mutableStateOf(false)}
    Column {
        Surface(
            modifier = modifier,
            shape = shape,
            border = if (rememberBorderShow.value) {
                if (isError) {
                    BorderStroke(1.dp, errorDefault)
                } else {
                    BorderStroke(1.dp, grayscaleBlack)
                }
            } else null,
            color = Color.Transparent,
            elevation = 0.dp
        ) {
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
                            placeholder, style = MaterialTheme.typography.body1.copy(
                                color = grayscale3,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = grayscaleBlack,
                    cursorColor = grayscaleBlack,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = if (!isError) {
                        if (rememberBorderShow.value) {
                            grayscaleOffWhite
                        } else {
                            grayscale5
                        }
                    } else errorLight,
                    leadingIconColor = pickledBlueWood,
                    errorCursorColor = errorLight,
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = grayscaleBlack,
                    fontWeight = FontWeight.Normal
                ),
                visualTransformation = if (isPasswordType) {
                    if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = modifier.fillMaxWidth()
                    .onFocusChanged {
                        rememberBorderShow.value = it.isFocused
                    },
                leadingIcon = leadingIcon,
                trailingIcon = {
                    if (isPasswordType) {
                        Icon(
                            painter = if (!passwordVisibility.value) painterResource(R.drawable.ic_eye) else painterResource(R.drawable.ic_eye_cross),
                            contentDescription = "",
                            tint = pickledBlueWood,
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
        }
        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (isError) error?.let {
            Text(
                it,
                style = MaterialTheme.typography.body2.copy(color = errorDefault),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
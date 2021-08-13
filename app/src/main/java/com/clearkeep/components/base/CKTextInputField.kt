package com.clearkeep.components.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
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
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.screen.chat.profile.ProfileViewModel

@Composable
fun CKTextInputField(
    placeholder: String,
    textValue: MutableState<String>?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    allowSpace: Boolean = true,
    maxChars: Int? = null
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
            border = if (rememberBorderShow.value && !readOnly) {
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
                value = textValue?.value ?: "",
                onValueChange = {
                    val trimmedInput = if (maxChars != null) {
                        if (it.length > maxChars) {
                            it.substring(0 until maxChars)
                        } else {
                            it
                        }
                    } else {
                        it
                    }
                    if (allowSpace) {
                        textValue?.value = trimmedInput
                    } else {
                        textValue?.value = trimmedInput.replace(" ", "")
                    }
                },
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
                    fontWeight = FontWeight.Normal,
                    fontSize = defaultNonScalableTextSize()
                ),
                visualTransformation = if (isPasswordType) {
                    if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        rememberBorderShow.value = it.isFocused
                    },
                leadingIcon = leadingIcon,
                trailingIcon = {
                    trailingIcon
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
                readOnly = readOnly
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
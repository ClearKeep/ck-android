package com.clearkeep.components.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

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
    onNext: () -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    allowSpace: Boolean = true,
    maxChars: Int? = null,
    onValueChange: (String) -> Unit = {}
) {
    val shape = MaterialTheme.shapes.large
    val isError = !error.isNullOrBlank()
    val focusManager = LocalFocusManager.current

    val isPasswordType = keyboardType == KeyboardType.Password
    val passwordVisibility = rememberSaveable { mutableStateOf(false) }

    val rememberBorderShow = rememberSaveable { mutableStateOf(false) }
    Column {
        Surface(
            modifier = modifier,
            shape = shape,
            border = if (rememberBorderShow.value && !readOnly) {
                val borderColor = when {
                    isError -> LocalColorMapping.current.error
                    else -> LocalColorMapping.current.bodyTextAlt
                }
                BorderStroke(1.sdp(), borderColor)
            } else null,
            color = Color.Transparent,
            elevation = 0.sdp()
        ) {
            TextField(
                value = textValue?.value ?: "",
                onValueChange = {
                    onValueChange(it)

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
                placeholder = {
                    if (placeholder.isNotBlank()) {
                        Text(
                            placeholder, style = MaterialTheme.typography.body1.copy(
                                color = LocalColorMapping.current.bodyTextAlt,
                                fontWeight = FontWeight.Normal,
                                fontSize = defaultNonScalableTextSize()
                            )
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = LocalColorMapping.current.bodyTextAlt,
                    cursorColor = LocalColorMapping.current.bodyTextAlt,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = if (LocalColorMapping.current.isDarkTheme) {
                        colorBackgroundTextFieldDark
                    } else {
                        if (!isError) {
                            if (rememberBorderShow.value) {
                                grayscaleOffWhite
                            } else {
                                grayscale5
                            }
                        } else LocalColorMapping.current.error
                    },
                    leadingIconColor = LocalColorMapping.current.textFieldIconColor,
                    errorCursorColor = LocalColorMapping.current.error,
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color =  LocalColorMapping.current.bodyTextAlt,
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
                    if (isPasswordType) {
                        Icon(
                            painter = if (!passwordVisibility.value) {
                                painterResource(R.drawable.ic_eye)
                            } else {
                                painterResource(R.drawable.ic_eye_cross)
                            },
                            contentDescription = "",
                            tint = LocalColorMapping.current.textFieldIconColor,
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        passwordVisibility.value = !passwordVisibility.value
                                    }
                                )
                                .size(dimensionResource(R.dimen._24sdp)),
                        )
                    }
                },
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardActions = KeyboardActions(onDone = {
                    if (imeAction == ImeAction.Done) focusManager.clearFocus()
                }, onNext = { if (imeAction == ImeAction.Next) onNext() }),
                keyboardOptions = KeyboardOptions(
                    imeAction = imeAction,
                    keyboardType = keyboardType
                ),
                readOnly = readOnly
            )
        }
        if (isError) {
            Spacer(modifier = Modifier.height(4.sdp()))
        }
        if (isError) error?.let {
            Text(
                it,
                style = MaterialTheme.typography.body2.copy(color = LocalColorMapping.current.error),
                modifier = Modifier.padding(start = 8.sdp())
            )
        }
    }
}
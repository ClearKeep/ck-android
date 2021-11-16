package com.clearkeep.presentation.components.base

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.presentation.components.*
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

@Composable
fun CKTextInputFieldChat(
    placeholder: String,
    textValue: State<String?>,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onChangeMessage: (String) -> Unit
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
            border = if (rememberBorderShow.value) {
                if (isError) {
                    BorderStroke(1.sdp(), errorDefault)
                } else {
                    BorderStroke(1.sdp(), grayscaleBlack)
                }
            } else null,
            color = Color.Transparent,
            elevation = 0.sdp()
        ) {
            TextField(
                value = textValue.value ?: "",
                onValueChange = { onChangeMessage.invoke(it) },
                placeholder = {
                    if (placeholder.isNotBlank()) {
                        Text(
                            placeholder, style = MaterialTheme.typography.body1.copy(
                                color = LocalColorMapping.current.bodyTextDisabled,
                                fontWeight = FontWeight.Normal,
                                fontSize = defaultNonScalableTextSize()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = LocalColorMapping.current.bodyTextAlt,
                    cursorColor = LocalColorMapping.current.bodyTextAlt,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = if (!isError) {
                        if (rememberBorderShow.value) {
                            LocalColorMapping.current.textFieldBackgroundAltFocused
                        } else {
                            LocalColorMapping.current.textFieldBackgroundAlt
                        }
                    } else LocalColorMapping.current.error,
                    leadingIconColor = pickledBlueWood,
                    errorCursorColor = LocalColorMapping.current.error,
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = LocalColorMapping.current.bodyTextAlt,
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
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = imeAction,
                    keyboardType = keyboardType
                ),
            )
        }
        if (isError) {
            Spacer(modifier = Modifier.height(4.sdp()))
        }
        if (isError) error?.let {
            CKText(
                it,
                style = MaterialTheme.typography.body2.copy(color = errorDefault),
                modifier = Modifier.padding(start = 8.sdp())
            )
        }
    }
}
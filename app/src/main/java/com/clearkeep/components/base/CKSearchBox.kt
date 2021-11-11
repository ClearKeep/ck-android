package com.clearkeep.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp

@Composable
fun CKSearchBox(
    textValue: MutableState<String>,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search),
    maxChars: Int? = null,
    focusRequester: FocusRequester? = null,
    isDarkModeInvertedColor: Boolean = false,
    onImeAction: () -> Unit = {}
) {
    val shape = MaterialTheme.shapes.large
    val focusManager = LocalFocusManager.current

    val keyboardFocusRequester = focusRequester ?: remember { FocusRequester() }

    val rememberBorderShow = rememberSaveable { mutableStateOf(false) }
    Column {
        Surface(
            modifier = modifier,
            shape = shape,
            border = if (rememberBorderShow.value) {
                BorderStroke(
                    dimensionResource(R.dimen._1sdp),
                    LocalColorMapping.current.bodyTextAlt
                )
            } else null,
            color = Color.Transparent,
            elevation = 0.sdp()
        ) {
            TextField(
                value = textValue.value,
                onValueChange = {
                    textValue.value = if (maxChars != null && it.length > maxChars) {
                        it.substring(0 until maxChars)
                    } else {
                        it
                    }
                },
                placeholder = {
                    CKText(
                        placeholder, style = MaterialTheme.typography.body1.copy(
                            color = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                                grayscaleDarkModeDarkGrey2
                            } else {
                                LocalColorMapping.current.descriptionText
                            },
                            fontWeight = FontWeight.Normal
                        )
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = LocalColorMapping.current.bodyTextAlt,
                    cursorColor = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                        grayscaleDarkModeDarkGrey2
                    } else {
                        LocalColorMapping.current.bodyTextAlt
                    },
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                        Color(0xFF9E9E9E)
                    } else if (rememberBorderShow.value) {
                        LocalColorMapping.current.textFieldBackgroundAltFocused
                    } else {
                        LocalColorMapping.current.textFieldBackgroundAlt
                    },
                    leadingIconColor =  LocalColorMapping.current.textFieldIconColor,
                    trailingIconColor =  LocalColorMapping.current.textFieldIconColor,
                    errorCursorColor = LocalColorMapping.current.error
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                        grayscaleDarkModeDarkGrey2
                    } else {
                        LocalColorMapping.current.bodyTextAlt
                    },
                    fontSize = defaultNonScalableTextSize(),
                    fontWeight = FontWeight.Normal
                ),
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        rememberBorderShow.value = it.isFocused
                    }
                    .focusRequester(keyboardFocusRequester),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "",
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        tint = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                            grayscaleDarkModeDarkGrey2
                        } else {
                            LocalColorMapping.current.textFieldIconColor
                        }
                    )
                },
                trailingIcon = {
                    if (textValue.value.length > 1) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "",
                            modifier = Modifier
                                .clickable {
                                    textValue.value = ""
                                }
                                .size(dimensionResource(R.dimen._24sdp)),
                            tint = if (LocalColorMapping.current.isDarkTheme && isDarkModeInvertedColor) {
                                grayscaleDarkModeDarkGrey2
                            } else {
                                LocalColorMapping.current.textFieldIconColor
                            }
                        )
                    }
                },
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onImeAction.invoke()
                }),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
            )
        }
    }
}
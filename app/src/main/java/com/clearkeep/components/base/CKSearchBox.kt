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
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
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

@Composable
fun CKSearchBox(
    textValue: MutableState<String>,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search)
) {
    val shape = MaterialTheme.shapes.large
    val focusManager = LocalFocusManager.current

    var rememberBorderShow = remember { mutableStateOf(false) }
    Column {
        Surface(
            modifier = modifier,
            shape = shape,
            border = if (rememberBorderShow.value) {
                BorderStroke(dimensionResource(R.dimen._1sdp), MaterialTheme.colors.secondaryVariant)
            } else null,
            color = Color.Transparent,
            elevation = 0.dp
        ) {
            TextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                placeholder = {
                    CKText(
                        placeholder, style = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.secondaryVariant,
                    cursorColor = MaterialTheme.colors.secondaryVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = if (rememberBorderShow.value) {
                        grayscaleOffWhite
                    } else {
                        MaterialTheme.colors.secondary
                    },
                    leadingIconColor = MaterialTheme.colors.secondaryVariant,
                    trailingIconColor = MaterialTheme.colors.secondaryVariant,
                    errorCursorColor = MaterialTheme.colors.error,
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.secondaryVariant,
                    fontSize = defaultNonScalableTextSize(),
                    fontWeight = FontWeight.Normal
                ),
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        rememberBorderShow.value = it.isFocused
                    },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "",
                        Modifier.size(dimensionResource(R.dimen._24sdp))
                    )
                },
                trailingIcon = {
                    if (textValue.value.length > 1) {
                        Icon(Icons.Filled.Close,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                textValue.value = ""
                            }.size(dimensionResource(R.dimen._24sdp)))
                    }
                },
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text),
            )
        }
    }
}
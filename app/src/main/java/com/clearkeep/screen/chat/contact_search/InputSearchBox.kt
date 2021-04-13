package com.clearkeep.screen.chat.contact_search

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle

@Composable
fun InputSearchBox(
    onValueChange: (text: String) -> Unit,
    onClearClick: (() -> Unit)
) {
    val userName = remember {mutableStateOf("")}
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = userName.value,
        onValueChange = {
            userName.value = it
            onValueChange(it.trim())
        },
        placeholder = {
            Text(
                "search...", style = MaterialTheme.typography.body2.copy(
                    color = Color(0xFF8D8C8C)
                )
            )
            },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            cursorColor = MaterialTheme.colors.surface,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.body2.copy(
            color = MaterialTheme.colors.surface
        ),
        trailingIcon = {
            if (userName.value.length>1){
                Icon(Icons.Filled.Close, contentDescription = ""
                    ,tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.clickable {
                        onClearClick.invoke()
                        userName.value = ""
                    })
            }else{
                Icon(Icons.Filled.Close, contentDescription = ""
                    ,tint = Color.White)
            }

        }
    )
}
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
            onValueChange(it)
        },
        placeholder = { Text(text = "search...", style = TextStyle(color = Color(0xFF8D8C8C))) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            cursorColor = Color.Blue,
            focusedBorderColor = Color.Blue,
            unfocusedBorderColor = Color.Gray,
        ),
        textStyle = MaterialTheme.typography.body2.copy(color = Color.Black),
        trailingIcon = {
            if (userName.value.length>1){
                Icon(Icons.Filled.Close, contentDescription = ""
                    ,tint = colorResource(id = R.color.material_grey_900),
                    modifier = Modifier.clickable {
                        onClearClick.invoke()
                        userName.value = ""
                    })
            }else{
                Icon(Icons.Filled.Close, contentDescription = ""
                    ,tint = colorResource(id = R.color.foreground_material_dark))
            }

        }
    )
}
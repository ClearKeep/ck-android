package com.clearkeep.screen.chat.main.composes

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.TextSemanticsProperties.ImeAction
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.clearkeep.components.colorIron

@Composable
fun InputSearchBox(
        enabled: Boolean = true,
        onValueChange: (text: String) -> Unit
) {
    if (enabled) {
        EditableSearchBox(onValueChange)
    } else {
        StaticSearchBox()
    }
}

@Composable
fun StaticSearchBox() {
    Card(shape = RoundedCornerShape(8.dp), backgroundColor = colorIron){
        Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(asset = Icons.Outlined.Search, tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text ="search", style = MaterialTheme.typography.body2.copy(
                    color = Color.White
            ))
        }
    }
}

@Composable
fun EditableSearchBox(
    onValueChange: (text: String) -> Unit
) {
    val userName = state { "" }
    TextField(
        value = userName.value,
        onValueChange = {
            userName.value = it
            onValueChange(it)
        },
        label = {
            Text(
                "search", style = MaterialTheme.typography.body2.copy(
                    color = Color.Gray
                )
            )
        },
        placeholder = { Text(" ") },
        activeColor = Color.Red,
        inactiveColor = Color.Blue,
        textStyle = MaterialTheme.typography.body2.copy(
            color = Color.Blue
        )
    )
}
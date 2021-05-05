package com.clearkeep.components.base

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun CkTextNormalHome(
    text: String,
    color: Color? = MaterialTheme.colors.onBackground,
    fontSize: TextUnit = 14.sp,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier

) {
    Text(
        text = text, modifier = modifier, style = TextStyle(
            color = color ?: MaterialTheme.colors.onBackground,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    )
}

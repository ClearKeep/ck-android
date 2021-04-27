package com.clearkeep.components.base

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.components.primaryDefault

@Composable
fun CKTextButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onClick: () -> Unit,
    enabled: Boolean = true,
    fontSize: TextUnit = 12.sp,
    textButtonType: TextButtonType = TextButtonType.White
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = getTextContentColor(textButtonType)
        ),
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(title,
            style = MaterialTheme.typography.body1.copy(
                fontSize = fontSize
            )
        )
    }
}

private fun getTextContentColor(textButtonType: TextButtonType): Color {
    return when(textButtonType) {
        TextButtonType.White -> grayscaleOffWhite
        TextButtonType.Blue -> primaryDefault
    }
}

enum class TextButtonType {
    White,
    Blue,
}
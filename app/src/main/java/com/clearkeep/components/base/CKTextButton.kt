package com.clearkeep.components.base

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit
import com.clearkeep.R
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun CKTextButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onClick: () -> Unit,
    enabled: Boolean = true,
    fontSize: TextUnit = dimensionResource(R.dimen._12sdp).toNonScalableTextSize(),
    textButtonType: TextButtonType = TextButtonType.White
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = getTextContentColor(textButtonType),
            disabledContentColor = grayscale3
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

@Composable
private fun getTextContentColor(textButtonType: TextButtonType): Color {
    return when(textButtonType) {
        TextButtonType.White -> grayscaleOffWhite
        TextButtonType.Blue -> MaterialTheme.colors.primary
    }
}

enum class TextButtonType {
    White,
    Blue,
}
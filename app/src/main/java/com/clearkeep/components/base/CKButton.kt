package com.clearkeep.components.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.utilities.defaultNonScalableTextSize

@Composable
fun CKButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: ButtonType = ButtonType.Blue
) {
    val radius = dimensionResource(R.dimen._20sdp)
    val height = dimensionResource(R.dimen._40sdp)

    OutlinedButton(
        onClick = {
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(radius),
        border = if (buttonType == ButtonType.BorderWhite) {
            BorderStroke(
                2.dp,
                grayscaleOffWhite
            )
        } else if (buttonType == ButtonType.BorderGradient) {
            BorderStroke(
                2.dp, Brush.horizontalGradient(
                    colors = listOf(
                        backgroundGradientStart,
                        backgroundGradientEnd
                    )
                )
                )
        } else null,
        modifier = modifier.height(height),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (enabled) getButtonBackgroundColor(buttonType) else getButtonDisabledBackgroundColor(buttonType),
            contentColor = getButtonTextColor(buttonType),
            disabledContentColor = getDisabledButtonTextColor(buttonType)
        ),
    ) {
        Box(
            modifier = if (buttonType == ButtonType.Blue) Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(
                                backgroundGradientStart,
                                backgroundGradientEnd
                            )
                        } else {
                            listOf(
                                backgroundGradientStartHalfTransparent,
                                backgroundGradientEndHalfTransparent
                            )
                        }
                    )
                )
                .then(modifier)
                .height(height) else Modifier
                .fillMaxWidth()
                .then(modifier)
                .height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.body1.copy(
                    fontSize = defaultNonScalableTextSize()
                )
            )
        }
    }
}

private fun getButtonTextColor(buttonType: ButtonType): Color {
    return when(buttonType) {
        ButtonType.White -> primaryDefault
        ButtonType.Blue -> grayscaleBackground
        ButtonType.Red -> primaryDefault
        ButtonType.BorderWhite -> grayscaleOffWhite
        ButtonType.BorderGradient -> backgroundGradientStart
    }
}

private fun getDisabledButtonTextColor(buttonType: ButtonType): Color {
    return when(buttonType) {
        ButtonType.White -> primaryDefault
        ButtonType.Blue -> grayscaleBackground
        ButtonType.Red -> primaryDefault
        ButtonType.BorderWhite -> grayscaleOffWhite
        ButtonType.BorderGradient -> backgroundGradientStart
    }
}

private fun getButtonBackgroundColor(buttonType: ButtonType): Color {
    return when(buttonType) {
        ButtonType.White -> grayscaleOffWhite
        ButtonType.Blue -> Color.Transparent
        ButtonType.Red -> grayscaleOffWhite
        ButtonType.BorderWhite -> Color.Transparent
        ButtonType.BorderGradient -> Color.Transparent
    }
}

private fun getButtonDisabledBackgroundColor(buttonType: ButtonType): Color {
    return when (buttonType) {
        ButtonType.White, ButtonType.Red -> Color(0x80FFFFFF)
        else -> Color.Transparent
    }
}

enum class ButtonType {
    White,
    Blue,
    Red,
    BorderWhite,
    BorderGradient,
}
package com.clearkeep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val lightThemeColors = lightColors(
    primary = primaryDefault,
    primaryVariant = grayscaleOffWhite,
    onPrimary = grayscaleOffWhite,

    /*secondary = Color.LightGray,
    secondaryVariant = Color.Black,
    onSecondary = Color.LightGray,*/

    background = grayscaleBackground,
    onBackground = grayscaleBlack,

    surface = colorLightBlue,
    onSurface = Color.White,

    error = errorLight,
    onError = errorDefault
)

val darkThemeColors = darkColors(
    primary = Color.Black,
    primaryVariant = Color.White,
    onPrimary = Color.White,

    /*secondary = Color.DarkGray,
    secondaryVariant = Color.White,
    onSecondary = Color.DarkGray,*/

    background = Color.Black,
    onBackground = Color.DarkGray,

    surface = Color.White,
    onSurface = Color.White,

    error = Color(0xFFD00036),
    onError = Color.White
)

@Composable
fun CKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    children: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) darkThemeColors else lightThemeColors,
        shapes = Shapes,
        typography = ckTypography
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                )
        ) {
            children()
        }
    }
}

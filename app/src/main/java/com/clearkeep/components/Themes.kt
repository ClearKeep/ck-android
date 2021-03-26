package com.clearkeep.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val lightThemeColors = lightColors(
    primary = Color.White,
    primaryVariant = Color.Black,
    onPrimary = Color.Black,

    secondary = Color.LightGray,
    secondaryVariant = Color.Black,
    onSecondary = Color.LightGray,

    background = Color.White,
    onBackground = Color.Black,

    surface = Color.Blue,
    onSurface = Color.White,

    error = Color(0xFFD00036),
    onError = Color.White
)

/**
 * Note: Dark Theme support is not yet available, it will come in 2020. This is just an example of
 * using dark colors.
 */
val darkThemeColors = darkColors(
    primary = Color.Black,
    primaryVariant = Color.Blue,
    onPrimary = Color.White,

    secondary = Color.DarkGray,
    secondaryVariant = Color.White,
    onSecondary = Color.DarkGray,

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
        // TODO: update dark mode and remove surface color when have design
        children()
    }
}

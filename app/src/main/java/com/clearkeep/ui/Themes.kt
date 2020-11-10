package com.clearkeep.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ckDividerColor = Color.LightGray

val lightThemeColors = lightColors(
        primary = Color.White,
        primaryVariant = Color.Black,
        onPrimary = Color.Black,

        secondary = Color.Green,
        onSecondary = Color.Black,

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
    primary = Color(0xFFEA6D7E),
    primaryVariant = Color(0xFFDD0D3E),
    onPrimary = Color.Black,
    secondary = Color(0xFF121212),
    onSecondary = Color.White,
    surface = Color(0xFF121212),
    background = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun CKTheme(children: @Composable () -> Unit) {
    MaterialTheme(colors = lightThemeColors, typography = ckTypography) {
        children()
    }
}

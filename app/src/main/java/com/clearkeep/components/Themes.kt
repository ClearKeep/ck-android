package com.clearkeep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val lightThemeColors = lightColors(
    primary = primaryDefault,
    primaryVariant = grayscaleOffWhite,
    onPrimary = grayscaleOffWhite,

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

    background = colorBackgroundDark,
    onBackground = Color.White,

    surface = colorLightBlue,
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
        CompositionLocalProvider(LocalColorMapping provides provideColor(darkTheme)) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = LocalColorMapping.current.backgroundBrush
                        )
                    )
            ) {
                children()
            }
        }
    }
}

@Composable
fun CKInsetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    children: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Transparent, true)
    }

    MaterialTheme(
        colors = if (darkTheme) darkThemeColors else lightThemeColors,
        shapes = Shapes,
        typography = ckTypography
    ) {
        CompositionLocalProvider(LocalColorMapping provides provideColor(darkTheme)) {
            ProvideWindowInsets {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = LocalColorMapping.current.backgroundBrush
                            )
                        )
                ) {
                    children()
                }
            }
        }
    }
}

data class ColorMapping(
    val warning: Color = colorWarningLight,
    val error: Color = errorDefault,
    val backgroundBrush: List<Color> = listOf(
        backgroundGradientStart,
        backgroundGradientEnd,
    ),
    val iconColorFilter: ColorFilter? = null,
    val topAppBarTitle: Color = grayscaleOffWhite,
    val topAppBarContent: Color = grayscaleBackground,
    val bodyText: Color = Color.White,
    val clickableBodyText: Color = Color.White
)

val LocalColorMapping = compositionLocalOf { ColorMapping() }

fun provideColor(darkTheme: Boolean) = if (darkTheme) {
    ColorMapping(
        primaryDefault,
        primaryDefault,
        listOf(colorBackgroundDark, colorBackgroundDark),
        ColorFilter.tint(colorTextDark),
        grayscaleDarkModeGreyLight,
        grayscaleDarkModeGreyLight,
        grayscaleDarkModeGreyLight,
        clickableBodyText = primaryDefault,
    )
} else {
    ColorMapping()
}
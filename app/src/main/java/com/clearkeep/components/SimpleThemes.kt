package com.clearkeep.components

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@SuppressLint("ConflictingOnColor")
val simpleLightThemeColors = lightColors(
    primary = primaryDefault,
    primaryVariant = grayscale1,
    onPrimary = Color.White,

    secondary = grayscale5,
    secondaryVariant = grayscale1,
    onSecondary = grayscale3,

    background = grayscaleOffWhite,
    onBackground = grayscale2,

    surface = colorSuccessDefault,
    onSurface = Color.White,

    error = errorDefault,
    onError = Color.White
)

@SuppressLint("ConflictingOnColor")
val simpleDarkThemeColors = darkColors(
    primary = grayscaleOffWhite,
    primaryVariant = Color.White,
    onPrimary = grayscaleBlack,

    secondary = grayscale5,
    secondaryVariant = grayscale1,
    onSecondary = grayscale3,

    background = grayscaleBlack,
    onBackground = Color.White,

    surface = colorSuccessDefault,
    onSurface = Color.White,

    error = errorDefault,
    onError = Color.White
)

@Composable
fun CKSimpleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    children: @Composable () -> Unit
) {
    //todo disable dark mode
    MaterialTheme(
        colors = if (darkTheme) simpleLightThemeColors else simpleLightThemeColors,
        shapes = Shapes,
        typography = ckTypography
    ) {
        Surface(color= Color.White) {
            children()
        }
    }
}

@Composable
fun CKSimpleInsetTheme(children: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Transparent, true)
    }

    CKSimpleTheme {
        ProvideWindowInsets {
            children()
        }
    }
}
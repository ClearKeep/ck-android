package com.clearkeep.components

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.google.accompanist.insets.ProvideWindowInsets

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
    val localFocusManager = LocalFocusManager.current

    MaterialTheme(
        colors = if (darkTheme) simpleDarkThemeColors else simpleLightThemeColors,
        shapes = Shapes,
        typography = ckTypography
    ) {
        CompositionLocalProvider(LocalColorMapping provides provideColor(darkTheme)) {
            Surface(
                color = if (darkTheme) colorBackgroundDark else Color.White,
                modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
                }
            ) {
                children()
            }
        }
    }
}

@Composable
fun CKSimpleInsetTheme(enabled: Boolean = true, children: @Composable () -> Unit) {
    CKSimpleTheme {
        if (enabled) {
            ProvideWindowInsets {
                children()
            }
        } else {
            children()
        }
    }
}
package com.clearkeep.common.presentation.components.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.common.presentation.components.grayscaleDarkModeDarkGrey2
import com.clearkeep.common.presentation.components.primaryDefault

@Composable
fun CKTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    isDarkTheme: Boolean = false
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        backgroundColor = if (isDarkTheme) {
            grayscaleDarkModeDarkGrey2
        } else {
            primaryDefault
        },
        elevation = 0.dp
    )
}
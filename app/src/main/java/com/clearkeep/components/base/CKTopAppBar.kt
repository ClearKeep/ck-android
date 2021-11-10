package com.clearkeep.components.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.LocalColorMapping
import com.clearkeep.components.grayscaleDarkModeDarkGrey2
import com.clearkeep.components.primaryDefault
import com.clearkeep.utilities.printlnCK

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
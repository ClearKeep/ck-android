package com.clearkeep.components.base

import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CKDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp,
) {
    Divider(
        color = MaterialTheme.colors.onSecondary,
        thickness = thickness,
        modifier = modifier,
        startIndent = startIndent
    )
}
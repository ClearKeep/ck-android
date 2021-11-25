package com.clearkeep.common.presentation.components.base

import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.clearkeep.common.utilities.sdp

@Composable
fun CKDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.sdp(),
    startIndent: Dp = 0.sdp(),
) {
    Divider(
        color = MaterialTheme.colors.onSecondary,
        thickness = thickness,
        modifier = modifier,
        startIndent = startIndent
    )
}
package com.clearkeep.components.base

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CKTextButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor =
            MaterialTheme.colors.surface
        ),
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(title)
    }
}
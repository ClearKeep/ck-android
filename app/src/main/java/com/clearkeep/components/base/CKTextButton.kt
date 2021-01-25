package com.clearkeep.components.base

import androidx.compose.foundation.Text
import androidx.compose.material.ButtonConstants
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CKTextButton(
        title: String = "",
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
) {
    TextButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonConstants.defaultTextButtonColors(contentColor = MaterialTheme.colors.surface),
            enabled = enabled
    ) {
        Text(title)
    }
}
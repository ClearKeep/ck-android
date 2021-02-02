package com.clearkeep.components.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedButton
import androidx.compose.ui.unit.dp

@Composable
fun CKButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier.padding(0.dp),
        enabled: Boolean = true,
) {
    OutlinedButton(
            onClick = {
                onClick()
            },
            enabled = enabled,
            modifier = modifier
    ) {
        Text(
                text = label,
        )
    }
}
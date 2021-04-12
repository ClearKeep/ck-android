package com.clearkeep.components.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text

@Composable
fun CKButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        buttonType: ButtonType = ButtonType.Blue
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

enum class ButtonType {
    White,
    Blue,
    Red,
    BorderWhite,
}
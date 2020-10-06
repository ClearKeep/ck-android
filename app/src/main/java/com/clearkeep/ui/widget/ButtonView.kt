package com.clearkeep.ui.widget

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.RowScope.weight
import androidx.ui.layout.padding
import androidx.ui.material.OutlinedButton
import androidx.ui.unit.dp

@Composable
fun ButtonGeneral(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = {
            onClick()
        },
        modifier = Modifier.padding(16.dp) + Modifier.weight(1f)
    ) {
        Text(
            text = label,
            color = Color.Blue,
            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
        )
    }
}
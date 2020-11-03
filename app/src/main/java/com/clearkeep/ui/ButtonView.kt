package com.clearkeep.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ButtonGeneral(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = {
            onClick()
        },
        modifier = Modifier.padding(16.dp)// + Modifier.weight(1f)
    ) {
        Text(
            text = label,
            color = Color.Blue,
            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
        )
    }
}
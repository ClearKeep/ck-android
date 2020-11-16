package com.clearkeep.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable

@Composable
fun CKScaffold(children: @Composable () -> Unit) {
    CKTheme {
        Surface(color = MaterialTheme.colors.primary) {
            children()
        }
    }
}

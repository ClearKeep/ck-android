package com.clearkeep.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun CKAlertDialog(
        onDismissRequest: (() -> Unit) = { },
        confirmButton: @Composable (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        dismissButton: @Composable (() -> Unit)? = null,
        title: @Composable (() -> Unit)? = null,
        text: @Composable (() -> Unit)? = null,
        shape: Shape = RoundedCornerShape(8.dp),
        backgroundColor: Color = Color.White,
        contentColor: Color = Color.Black,
        properties: DialogProperties? = null
) {
    AlertDialog(
            onDismissRequest = onDismissRequest,
            buttons = {
                @OptIn(ExperimentalLayout::class)
                (Box(Modifier.fillMaxWidth().padding(all = 8.dp)) {
                    FlowRow(
                            mainAxisSize = SizeMode.Expand,
                            mainAxisAlignment = MainAxisAlignment.End,
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 12.dp
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                })
            },
            modifier = modifier,
            title = title,
            text = text,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            properties = properties
    )
}
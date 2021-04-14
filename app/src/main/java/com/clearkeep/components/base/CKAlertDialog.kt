package com.clearkeep.components.base

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun CKAlertDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmTitle: String? = null,
    dismissTitle: String? = null,
    onConfirmButtonClick: (() -> Unit) ? = null,
    onDismissButtonClick: (() -> Unit) ? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = Color.White,
    contentColor: Color = Color.Black,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            onConfirmButtonClick?.let { onClick ->
                DialogButton(confirmTitle ?: "Confirm", onClick)
            }
        },
        dismissButton = {
            onDismissButtonClick?.let { onClick ->
                DialogButton(dismissTitle ?: "OK", onClick)
            }
        },
        modifier = modifier,
        title = {
            Text(title ?: "")
        },
        text = {
            Text(text ?: "")
        },
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )
}

@Composable
fun DialogButton(title: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colors.surface
        ),
    ) {
        Text(title.toUpperCase(),
            /*style = MaterialTheme.typography.body1.copy(
                fontSize = 12.sp
            )*/
        )
    }
}

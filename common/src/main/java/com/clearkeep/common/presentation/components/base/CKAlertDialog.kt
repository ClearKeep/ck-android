package com.clearkeep.common.presentation.components.base

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.clearkeep.common.R
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.presentation.components.colorSuccessDefault
import java.util.*

@Composable
fun CKAlertDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmTitle: String? = null,
    dismissTitle: String? = null,
    onConfirmButtonClick: (() -> Unit)? = null,
    onDismissButtonClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.sdp()),
    backgroundColor: Color = Color.White,
    contentColor: Color = Color.Black,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            onConfirmButtonClick?.let { onClick ->
                DialogButton(confirmTitle ?: stringResource(R.string.confirm), onClick)
            }
        },
        dismissButton = {
            onDismissButtonClick?.let { onClick ->
                DialogButton(dismissTitle ?: stringResource(R.string.ok), onClick)
            }
        },
        modifier = modifier,
        title = {
            Text(title ?: "", textAlign = TextAlign.Justify)
        },
        text = if (text.isNullOrBlank()) null else ({
            Text(text)
        }),
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
            contentColor = colorSuccessDefault
        ),
    ) {
        Text(title.toUpperCase(Locale.ROOT))
    }
}

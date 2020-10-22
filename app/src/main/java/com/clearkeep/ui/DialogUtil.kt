package com.clearkeep.ui

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.foundation.Text
import androidx.ui.material.AlertDialog
import androidx.ui.material.AlertDialogButtonLayout
import androidx.ui.material.Button

@Composable
fun SideBySideAlertDialogSample(title: String,message: String) {
    val openDialog = state { true }

    if (openDialog.value) {
        AlertDialog(
            onCloseRequest = {
                // Because we are not setting openDialog.value to false here,
                // the user can close this dialog only via one of the buttons we provide.
            },
            title = {
                Text(text = title)
            },
            text = {
                Text(message)
            },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Ok")
                }
            },
            dismissButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Cancel")
                }
            },
            buttonLayout = AlertDialogButtonLayout.SideBySide
        )
    }
}
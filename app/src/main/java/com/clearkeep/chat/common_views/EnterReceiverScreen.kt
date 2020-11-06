package com.clearkeep.chat.common_views

import android.text.TextUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.navigation.NavHostController
import com.clearkeep.chat.single.SingleChatViewModel
import com.clearkeep.ui.ButtonGeneral
import com.clearkeep.ui.FilledTextInputComponent

@Composable
fun EnterReceiverScreen(
    navController: NavHostController,
    singleChatViewModel: SingleChatViewModel
) {
    val remoteUser = state { "" }
    Column {
        FilledTextInputComponent(
            "Recipient",
            " ",
            remoteUser
        )
        ButtonGeneral(
            "Continue",
            onClick = {
                if (!validateInput(remoteUser.value)) {
                    return@ButtonGeneral
                }
                singleChatViewModel.insertSingleRoom(remoteUser.value)
                navController.popBackStack(navController.graph.startDestination, false)
            })
    }
}

fun validateInput(receiverId: String) : Boolean {
    if (TextUtils.isEmpty(receiverId)) {
        return false
    }

    return true
}
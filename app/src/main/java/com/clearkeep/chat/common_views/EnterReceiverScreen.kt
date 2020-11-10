package com.clearkeep.chat.common_views

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.navigation.NavHostController
import com.clearkeep.chat.single.SingleChatViewModel
import com.clearkeep.ui.base.CKButton
import com.clearkeep.ui.base.CKTextField

@Composable
fun EnterReceiverScreen(
    navController: NavHostController,
    singleChatViewModel: SingleChatViewModel
) {
    val remoteUser = state { "" }
    Column {
        TopAppBar(
                title = {
                    Text(text = "Enter receiver")
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                navController.popBackStack(navController.graph.startDestination, false)
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        CKTextField(
            "Recipient",
            " ",
            remoteUser
        )
        CKButton(
            "Continue",
            onClick = {
                if (!validateInput(remoteUser.value)) {
                    return@CKButton
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
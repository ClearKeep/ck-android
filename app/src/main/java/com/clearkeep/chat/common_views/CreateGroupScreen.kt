package com.clearkeep.chat.common_views

import android.text.TextUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.navigation.NavHostController
import com.clearkeep.chat.group.GroupChatViewModel
import com.clearkeep.ui.ButtonGeneral
import com.clearkeep.ui.FilledTextInputComponent

@Composable
fun CreateGroupScreen(
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel
) {
    val groupName = state { "" }
    val remoteUser = state { "" }
    Column {
        FilledTextInputComponent(
            "Group Name",
            " ",
            groupName
        )
        FilledTextInputComponent(
            "Group Id",
            " ",
            remoteUser
        )
        ButtonGeneral(
            "Continue",
            onClick = {
                if (!validateInput(groupName.value, remoteUser.value)) {
                    return@ButtonGeneral
                }
                groupChatViewModel.insertGroupRoom(groupName.value, remoteUser.value)
                navController.popBackStack(navController.graph.startDestination, false)
            })
    }
}

fun validateInput(groupName: String, remoteId: String) : Boolean {
    if (TextUtils.isEmpty(remoteId)) {
        return false
    }

    if (TextUtils.isEmpty(groupName)) {
        return false
    }

    return true
}
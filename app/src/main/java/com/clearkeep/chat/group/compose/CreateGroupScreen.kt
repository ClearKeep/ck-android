package com.clearkeep.chat.group.compose

import android.text.TextUtils
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.chat.group.GroupChatViewModel
import com.clearkeep.ui.base.CKButton
import com.clearkeep.ui.base.CKTextField

@Composable
fun CreateGroupScreen(
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel
) {
    val groupName = state { "" }
    val remoteUser = state { "" }
    Column {
        TopAppBar(
                title = {
                    Text(text = "Create group")
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
        Spacer(modifier = Modifier.height(40.dp))
        Column (modifier = Modifier.padding(horizontal = 20.dp)) {
            CKTextField(
                    "Group Name",
                    " ",
                    groupName,
            )
            Spacer(modifier = Modifier.height(10.dp))
            CKTextField(
                    "Group Id",
                    " ",
                    remoteUser,
            )
            Spacer(modifier = Modifier.height(20.dp))
            CKButton(
                    "Continue",
                    onClick = {
                        if (!validateInput(groupName.value, remoteUser.value)) {
                            return@CKButton
                        }
                        groupChatViewModel.createNewGroup(groupName.value, remoteUser.value)
                        navController.popBackStack(navController.graph.startDestination, false)
                    })
        }
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
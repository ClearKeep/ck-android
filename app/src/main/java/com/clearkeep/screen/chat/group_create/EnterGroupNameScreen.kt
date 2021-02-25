package com.clearkeep.screen.chat.group_create

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTextField

@Composable
fun EnterGroupNameScreen(
        navController: NavHostController,
        createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = mutableStateOf("")

    Column {
        TopAppBar(
                title = {
                    Text(text = "Create group")
                },
                actions = {
                    CKTextButton(title = "Create",
                        onClick = {createGroupViewModel.createGroup(groupName.value)}
                    )
                }
        )
        Spacer(Modifier.height(30.dp))

        Column (modifier = Modifier.padding(horizontal = 20.dp)) {
            CKTextField(
                "Group name",
                "",
                groupName
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
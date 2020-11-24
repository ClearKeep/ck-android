package com.clearkeep.screen.chat.create_group

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField

@Composable
fun EnterGroupNameScreen(
        navController: NavHostController,
        createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = state { "" }

    Column {
        TopAppBar(
                title = {
                    Text(text = "Create group")
                },
                actions = {
                    Box(modifier = Modifier.clickable(onClick = {createGroupViewModel.createGroup(groupName.value)})) {
                        Text(text = "Continue")
                    }
                }
        )
        Spacer(Modifier.preferredHeight(30.dp))

        Column (modifier = Modifier.padding(horizontal = 20.dp)) {
            CKTextField(
                "Group name",
                "",
                groupName
            )
            Spacer(Modifier.preferredHeight(20.dp))
        }
    }
}
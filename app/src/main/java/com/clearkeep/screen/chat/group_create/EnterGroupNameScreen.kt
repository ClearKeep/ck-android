package com.clearkeep.screen.chat.group_create

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTextField
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun EnterGroupNameScreen(
    navController: NavHostController,
    createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = remember {mutableStateOf("")}
    val createGroupState = createGroupViewModel.createGroupState.observeAsState()
    Column {
        CKTopAppBar(
            title = {
                Text(text = "Create group")
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navController.popBackStack(navController.graph.startDestination, false)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            },
            actions = {
                CKTextButton(
                    title = "Create",
                    onClick = { createGroupViewModel.createGroup(groupName.value.trim()) },
                    enabled = groupName.value.isNotBlank() && createGroupState.value != CreateGroupProcessing
                )
            }
        )
        Spacer(Modifier.height(30.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            CKTextField(
                "Group name",
                "Group name",
                groupName
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
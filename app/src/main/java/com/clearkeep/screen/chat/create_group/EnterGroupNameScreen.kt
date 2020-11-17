package com.clearkeep.screen.chat.create_group

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField

@Composable
fun EnterGroupNameScreen(
    createGroupViewModel: CreateGroupViewModel,
) {
    val groupName = state { "" }

    Column {
        TopAppBar(
            title = {
                Text(text = "Create group")
            },
            actions = {
                Text(text = "Continue")
            }
        )
        Spacer(Modifier.preferredHeight(30.dp))

        Column (modifier = Modifier.padding(horizontal = 20.dp)) {
            CKTextField(
                "Username",
                "",
                groupName
            )
            Spacer(Modifier.preferredHeight(20.dp))
            CKButton(
                stringResource(R.string.btn_login),
                onClick = {
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }
}
package com.clearkeep.screen.chat.main.profile

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKButton

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    Column {
        TopAppBar(
                title = {
                    Text(text = "Profile")
                },
        )
        Spacer(Modifier.preferredHeight(20.dp))
        CKButton(
            "Log out",
            onClick = {
                onLogout()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
        )
    }
}
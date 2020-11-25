package com.clearkeep.screen.chat.main.profile

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val profile = profileViewModel.profile.observeAsState()
    Column {
        CKTopAppBar(
                title = {
                    Text(text = "Profile")
                },
        )
        Spacer(Modifier.preferredHeight(60.dp))
        profile?.value?.let { user ->
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "username: ")
                Text(text = user.userName ?: "")
            }
        }
    }
}
package com.clearkeep.screen.chat.main.profile

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.screen.chat.main.composes.CircleAvatar

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val profile = profileViewModel.profile.observeAsState()
    Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CKTopAppBar(
                title = {
                    Text(text = "Profile")
                },
        )
        Spacer(Modifier.preferredHeight(60.dp))
        CircleAvatar("", size = 72.dp)
        Spacer(Modifier.preferredHeight(20.dp))
        profile?.value?.let { user ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = user.userName ?: "", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = user.email ?: "", style = MaterialTheme.typography.body2)
            }
        }
    }
}
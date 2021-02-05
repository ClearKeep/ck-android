package com.clearkeep.screen.chat.home.profile

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.screen.chat.home.HomeViewModel
import com.clearkeep.screen.chat.home.composes.CircleAvatar

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    homeViewModel: HomeViewModel,
    onLogout: () -> Unit,
) {
    val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

    val profile = profileViewModel.profile.observeAsState()
    val isLogoutProcessing = homeViewModel.isLogOutProcessing.observeAsState()

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
        Spacer(Modifier.preferredHeight(40.dp))
        isLogoutProcessing.value.let {
            if (it == null || it == false) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).clickable(onClick = {
                        setShowReminderDialog(true)
                    }),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(Icons.Outlined.Login)
                    Spacer(modifier = Modifier.width(15.dp))
                    Text("Logout")
                }
            }
        }
        LogoutConfirmDialog(showReminder, setShowReminderDialog, onLogout)
    }
}

@Composable
fun LogoutConfirmDialog(
        showReminder: Boolean,
        setShowDialog: (Boolean) -> Unit,
        onLogout: () -> Unit,
) {
    if (showReminder) {
        CKAlertDialog(
                title = {
                    Text("Logout")
                },
                text = {
                    Text("Do you want to log out?")
                },
                dismissButton = {
                    Button(
                            onClick = {
                                setShowDialog(false)
                            },
                    ) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(
                            onClick = {
                                setShowDialog(false)
                                onLogout()
                            },
                    ) {
                        Text("Ok")
                    }
                },
        )
    }
}
package com.clearkeep.screen.chat.home.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.BuildConfig
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.screen.chat.home.MainViewModel
import com.clearkeep.screen.chat.home.composes.CircleAvatar

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    homeViewModel: MainViewModel,
    onLogout: () -> Unit,
) {
    val versionName = BuildConfig.VERSION_NAME
    val env = BuildConfig.FLAVOR
    val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

    val profile = profileViewModel.profile.observeAsState()
    val isLogoutProcessing = homeViewModel.isLogOutProcessing.observeAsState()

    Box(

    ) {
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CKTopAppBar(
                title = {
                    Text(text = "Profile")
                },
            )
            profile?.value?.let { user ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(60.dp))
                    CircleAvatar(emptyList(), user.userName ?: "", size = 72.dp)
                    Spacer(Modifier.height(20.dp))
                    Text(text = user.userName ?: "", style = MaterialTheme.typography.h5)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = user.email ?: "", style = MaterialTheme.typography.body2)
                }
            }
            Spacer(Modifier.height(40.dp))
            isLogoutProcessing.value.let {
                if (it == null || it == false) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable(onClick = {
                                setShowReminderDialog(true)
                            }),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Login,
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.width(15.dp))
                        Text("Logout")
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text("version $versionName (${env.toUpperCase()})", style = MaterialTheme.typography.caption.copy(
            ))
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
            title = "Logout",
            text = "Do you want to log out?",
            onDismissButtonClick = {
                setShowDialog(false)
            },
            dismissTitle = "Cancel",
            onConfirmButtonClick = {
                setShowDialog(false)
                onLogout()
            },
        )
    }
}
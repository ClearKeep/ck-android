package com.clearkeep.screen.chat.main.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val profile = profileViewModel.profile.observeAsState()
    /*val image = imageResource(R.drawable.phone)
    val imageModifier = Modifier
            .preferredSize(100.dp)*/
    Column {
        TopAppBar(
                title = {
                    Text(text = "Profile")
                },
        )
        Spacer(Modifier.preferredHeight(60.dp))
        /*Box(Modifier.fillMaxWidth(), alignment = Alignment.TopCenter) {
            Image(image, imageModifier)
        }*/
        profile?.value?.let { user ->
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "username: ")
                Text(text = user.userName ?: "")
            }
        }
        Spacer(Modifier.preferredHeight(20.dp))
    }
}
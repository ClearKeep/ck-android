package com.clearkeep.screen.chat.room.composes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.clearkeep.R
import com.clearkeep.components.base.CKTextInputFieldChat
import com.clearkeep.components.grayscaleBackground

@ExperimentalComposeUiApi
@Composable
fun SendBottomCompose(
    navController: NavController,
    onSendMessage: (String) -> Unit,
) {
    val msgState = remember { mutableStateOf("") }
    val isKeyboardShow = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = grayscaleBackground)
    ) {
        IconButton(
            onClick = {
            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),

        ) {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    navController.navigate("image_picker")

                } else {
                }
            }

            Icon(
                painterResource(R.drawable.ic_photos),
                contentDescription = "",
                tint = MaterialTheme.colors.surface,
                modifier = Modifier.clickable {
                    if (isFilePermissionGranted(context)) {
                        navController.navigate("image_picker")
                    } else {
                        launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            )
        }

        IconButton(
            onClick = {

            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_at),
                contentDescription = "",
                tint = MaterialTheme.colors.surface,
            )
        }

        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            CKTextInputFieldChat(
                "Enter message...",
                msgState,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(painter = painterResource(id = R.drawable.ic_icon), contentDescription = null,
                            tint = MaterialTheme.colors.surface
                        )
                    }
                },)
        }
        IconButton(
            onClick = {
                if (!TextUtils.isEmpty(msgState.value)) {
                    val message = msgState.value
                    onSendMessage(message)
                    // clear text
                    msgState.value = ""
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .width(24.dp)
                .height(24.dp),
        ) {
            Icon(
                if (msgState.value == "") painterResource(R.drawable.ic_microphone) else painterResource(
                    id = R.drawable.ic_send_plane
                ),
                contentDescription = "",
                tint = MaterialTheme.colors.surface,
            )
        }
    }
}


private fun isFilePermissionGranted(context: Context) : Boolean{
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
            true
        }
        else -> {
            false
        }
    }
}
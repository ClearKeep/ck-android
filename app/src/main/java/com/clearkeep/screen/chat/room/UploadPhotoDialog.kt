package com.clearkeep.screen.chat.room

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.clearkeep.R
import com.clearkeep.components.base.CKText
import com.clearkeep.components.colorDialogScrim
import com.clearkeep.components.colorLightBlue
import com.clearkeep.components.separatorDarkNonOpaque
import com.clearkeep.components.tintsRedLight
import com.clearkeep.utilities.sdp

@ExperimentalComposeUiApi
@Composable
fun UploadPhotoDialog(
    isOpen: Boolean,
    getPhotoUri: () -> Uri,
    onDismiss: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onTakePhoto: () -> Unit
) {
    val context = LocalContext.current

    val requestStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onNavigateToAlbums()
        } else {
            onDismiss()
        }
    }

    val takePhotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSuccessful: Boolean ->
            if (isSuccessful) {
                onTakePhoto()
                onDismiss()
            }
        }

    val requestCameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                takePhotoLauncher.launch(getPhotoUri())
            } else {
                onDismiss()
            }
        }

    if (isOpen) {
        Box {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorDialogScrim)
                    .clickable {
                        onDismiss()
                    })
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.sdp())
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.sdp()))
                        .background(Color.White)
                ) {
                    CKText(
                        stringResource(R.string.take_photo),
                        Modifier
                            .fillMaxWidth()
                            .padding(16.sdp())
                            .clickable {
                                if (isCameraPermissionGranted(context)) {
                                    takePhotoLauncher.launch(getPhotoUri())
                                } else {
                                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                    Divider(color = separatorDarkNonOpaque)
                    CKText(
                        stringResource(R.string.albums),
                        Modifier
                            .fillMaxWidth()
                            .padding(16.sdp())
                            .clickable {
                                if (isFilePermissionGranted(context)) {
                                    onNavigateToAlbums()
                                } else {
                                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }, textAlign = TextAlign.Center, color = tintsRedLight
                    )
                }
                Spacer(Modifier.height(8.sdp()))
                Box {
                    CKText(
                        stringResource(R.string.cancel), modifier = Modifier
                            .clip(RoundedCornerShape(14.sdp()))
                            .background(Color.White)
                            .align(Alignment.Center)
                            .padding(16.sdp())
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                }
                Spacer(Modifier.height(14.sdp()))
            }
        }
    }
}

fun isFilePermissionGranted(context: Context): Boolean {
    return isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun isCameraPermissionGranted(context: Context): Boolean {
    return isPermissionGranted(context, Manifest.permission.CAMERA)
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, permission) -> {
            true
        }
        else -> {
            false
        }
    }
}
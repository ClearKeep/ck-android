package com.clearkeep.screen.chat.room.image_picker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import coil.imageLoader
import com.clearkeep.R
import com.clearkeep.components.CKSimpleInsetTheme
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.components.base.CKRadioButton
import com.clearkeep.components.base.CKText
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.components.primaryDefault
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding
import java.util.*

@ExperimentalFoundationApi
@Composable
fun ImagePickerScreen(
    imageUriSelected: LiveData<List<String>>,
    navController: NavController,
    onlyPickOne: Boolean = false,
    insetEnabled: Boolean = true,
    onSetSelectedImages: (uris: List<String>) -> Unit
) {
    val context = LocalContext.current
    val urisSelected =
        remember { imageUriSelected.value?.toMutableStateList() ?: mutableStateListOf() }

    val uris = getAllImages(context)
    printlnCK(uris.toString())

    CKSimpleInsetTheme(insetEnabled) {
        Column(if (insetEnabled) Modifier.navigationBarsPadding() else Modifier) {
            Box(
                Modifier
                    .statusBarsHeight()
                    .background(MaterialTheme.colors.primary)
                    .fillMaxWidth())
            CKTopAppBar(
                {},
                Modifier,
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(painterResource(R.drawable.ic_cross), null)
                    }
                },
                actions = {
                    if (!onlyPickOne) {
                        CKText(
                            stringResource(R.string.image_picker_file_count, urisSelected.size),
                            modifier = Modifier
                                .padding(end = 16.sdp())
                                .clickable {
                                    onSetSelectedImages(urisSelected)
                                    navController.popBackStack()
                                }
                        )
                    }
                })
            Box(Modifier.padding(16.sdp())) {
                LazyVerticalGrid(cells = GridCells.Fixed(2)) {
                    items(uris) {
                        ImageItem(
                            Modifier.fillParentMaxWidth(0.5f),
                            it.toString(),
                            urisSelected.contains(it.toString())
                        ) { uri: String, isSelected: Boolean ->
                            when {
                                onlyPickOne -> {
                                    onSetSelectedImages(listOf(uri))
                                    navController.popBackStack()
                                }
                                isSelected -> urisSelected.add(uri)
                                else -> urisSelected.remove(uri)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getAllImages(context: Context): List<Uri> {
    val imagesList = mutableListOf<Uri>()

    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
    )

    cursor.use {
        if (cursor != null) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                imagesList.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn)
                    )
                )
            }
        }
    }

    return imagesList
}

@Composable
fun ImageItem(
    modifier: Modifier,
    uri: String,
    isSelected: Boolean,
    onSelect: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier.then(
            Modifier
                .clip(RectangleShape)
                .aspectRatio(1f)
                .clickable {
                    onSelect(uri, !isSelected)
                }
                .then(if (isSelected) Modifier.border(2.sdp(), primaryDefault) else Modifier)
        )
    ) {
        Image(
            rememberCoilPainter(
                request = uri,
                imageLoader = context.imageLoader,
                previewPlaceholder = R.drawable.ic_cross
            ),
            null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.sdp(), end = 12.sdp())
        ) {
            Image(
                painter = painterResource(id = if (isSelected) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                "",
            )
        }
    }
}
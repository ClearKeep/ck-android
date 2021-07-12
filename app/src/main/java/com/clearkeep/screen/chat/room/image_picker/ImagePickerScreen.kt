package com.clearkeep.screen.chat.room.image_picker

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.clearkeep.R
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.components.base.CKRadioButton
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.utilities.printlnCK
import com.google.accompanist.glide.rememberGlidePainter

@ExperimentalFoundationApi
@Composable
fun ImagePickerScreen(
    navController: NavController
) {
    val context = LocalContext.current

    val uris = getAllImages(context)
    printlnCK(uris.toString())

    CKSimpleTheme {
        Column {
            CKTopAppBar({}, Modifier, navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(painterResource(R.drawable.ic_cross), null)
                }
            }, actions = { Text("Upload ()", modifier = Modifier.padding(end = 16.dp)) })

            Box(Modifier.padding(16.dp)) {
                LazyVerticalGrid(cells = GridCells.Fixed(2)) {
                    items(uris) {
                        ImageItem(
                            Modifier.fillParentMaxWidth(0.5f), it.toString(), true
                        ) {

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
fun ImageItem(modifier: Modifier, uri: String, isSelected: Boolean, onSelect: (String) -> Unit) {
    Box(
        modifier.then(
            Modifier
                .clip(RectangleShape)
                .aspectRatio(1f)
        )
    ) {
        Image(
            rememberGlidePainter(request = uri, previewPlaceholder = R.drawable.ic_cross),
            null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        CKRadioButton(
            selected = isSelected,
            onClick = { onSelect(uri) },
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp)
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun ImagePickerScreenPreview() {
    ImageItem(Modifier, "", false) {}
}
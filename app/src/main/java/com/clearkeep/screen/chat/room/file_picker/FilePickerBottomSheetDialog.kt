package com.clearkeep.screen.chat.room.file_picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearkeep.R

@ExperimentalMaterialApi
@Composable
fun FilePickerBottomSheetDialog() {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(42.dp))
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                Text("Your File", Modifier.align(Alignment.CenterStart))
                Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_plus), null)
                    Text("Add File")
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        LazyRow {

        }
        Button(onClick = {},
            Modifier
                .align(Alignment.CenterHorizontally)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp)), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(Color(0x80000000))) {
            Text("Next", color = Color.White)
        }
        Spacer(Modifier.height(41.dp))
    }
}

@Composable
fun FileItem(modifier: Modifier = Modifier, fileName: String, fileUri: String, isSelected: Boolean, onSelect: (uri: String) -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_file), null, Modifier.weight(1f))
        Spacer(Modifier.width(14.dp))
        Text(fileName, overflow = TextOverflow.Ellipsis, maxLines = 1, modifier = Modifier.weight(8f))
        Spacer(Modifier.width(14.dp))
        Image(
            painter = painterResource(id = if (isSelected) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
            "",
            Modifier.weight(1f)
        )
        Spacer(Modifier.width(40.dp))
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

@ExperimentalMaterialApi
@Composable
@Preview
fun FilePickerBottomSheetDialogPreview() {
    Column {
        FilePickerBottomSheetDialog()
    }
}

@Composable
@Preview
fun FileItemPreview() {
    Column {
        FileItem(Modifier, "Test test test test test test test test test test test test test test test test test test", fileUri = "", isSelected = false) {

        }
        FileItem(Modifier, "Test test test test test test test test test test test test test test test test test test test test test test test test test test", fileUri = "", isSelected = false) {

        }
        FileItem(Modifier, "Test test test", fileUri = "", isSelected = false) {

        }
        FileItem(Modifier, "Test test test test test test test test test test test test test test test test test test", fileUri = "", isSelected = false) {

        }
    }
}
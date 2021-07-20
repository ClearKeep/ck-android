package com.clearkeep.screen.chat.room.file_picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.components.separatorDarkNonOpaque
import com.clearkeep.utilities.printlnCK

@ExperimentalMaterialApi
@Composable
fun FilePickerBottomSheetDialog() {
    val addFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        val uri = it
        if (uri != null) {
            printlnCK(uri.toString())
        }
    }
    val filePickerMime = "*/*"

    Column(Modifier.padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(42.dp))
        Row(Modifier.fillMaxWidth().clickable {
            addFileLauncher.launch(arrayOf(filePickerMime))
        }) {
            Box(Modifier.fillMaxWidth()) {
                Text("Your File", Modifier.align(Alignment.CenterStart))
                Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_plus), null)
                    Text("Add File")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        LazyColumn {
            val files = listOf("Test", "Test", "Test", "Test")
            itemsIndexed(files) { _: Int, file: String ->
                FileItem(Modifier.padding(vertical = 16.dp), file, fileUri = "", isSelected = false) {

                }
                Divider(Modifier.height(1.dp), separatorDarkNonOpaque)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = {},
            Modifier
                .align(Alignment.CenterHorizontally)
                .height(52.dp)
                .width(210.dp)
                .clip(RoundedCornerShape(26.dp)), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(Color(0x80000000))) {
            Text("Next", color = Color.White)
        }
        Spacer(Modifier.height(41.dp))
    }
}

@Composable
fun FileItem(modifier: Modifier = Modifier, fileName: String, fileUri: String, isSelected: Boolean, onSelect: (uri: String) -> Unit) {
    ConstraintLayout(modifier.then(Modifier.fillMaxWidth())) {
        val (fileIcon, fileNameRef, checkBox) = createRefs()
        Image(painterResource(R.drawable.ic_file), null, Modifier.size(45.dp).constrainAs(fileIcon) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
        })
        Text(fileName, overflow = TextOverflow.Ellipsis, maxLines = 1, modifier = Modifier.constrainAs(fileNameRef) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(fileIcon.end, 14.dp)
            end.linkTo(checkBox.start, 14.dp)
            width = Dimension.fillToConstraints
        })
        Image(
            painter = painterResource(id = if (isSelected) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
            "",
            Modifier.size(32.dp).constrainAs(checkBox) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        )
    }
}
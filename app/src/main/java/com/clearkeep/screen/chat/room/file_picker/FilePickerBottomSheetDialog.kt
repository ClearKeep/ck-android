package com.clearkeep.screen.chat.room.file_picker

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.components.separatorDarkNonOpaque
import com.clearkeep.screen.chat.room.RoomViewModel

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun FilePickerBottomSheetDialog(roomViewModel: RoomViewModel, onClickNext: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    keyboardController?.hide()

    val context = LocalContext.current
    val addFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) {
                roomViewModel.addStagedFileUri(it)
            }
        }
    val filePickerMime = "*/*"
    val stagedFiles = roomViewModel.fileUriStaged.observeAsState()

    Column(Modifier.padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(42.dp))
        Row(
            Modifier
                .fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text("Your File", Modifier.align(Alignment.CenterStart))
                Row(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .clickable {
                            addFileLauncher.launch(arrayOf(filePickerMime))
                        }, verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_plus), null)
                    Text("Add File")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        if (!stagedFiles.value.isNullOrEmpty()) {
            LazyColumn {
                itemsIndexed(stagedFiles.value!!.entries.toList()) { _: Int, entry: Map.Entry<Uri, Boolean> ->
                    FileItem(
                        Modifier.padding(vertical = 16.dp),
                        roomViewModel.getFileName(context, entry.key),
                        isSelected = stagedFiles.value!![entry.key] ?: false
                    ) {
                        roomViewModel.toggleSelectedFile(entry.key)
                    }
                    Divider(Modifier.height(1.dp), separatorDarkNonOpaque)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onClickNext() },
            Modifier
                .align(Alignment.CenterHorizontally)
                .height(52.dp)
                .width(210.dp)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(Color(0x80000000))
        ) {
            Text("Next", color = Color.White)
        }
        Spacer(Modifier.height(41.dp))
    }
}

@Composable
fun FileItem(
    modifier: Modifier = Modifier,
    fileName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    ConstraintLayout(modifier.then(Modifier.fillMaxWidth())) {
        val (fileIcon, fileNameRef, checkBox) = createRefs()
        Image(painterResource(R.drawable.ic_file), null,
            Modifier
                .size(45.dp)
                .constrainAs(fileIcon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                })
        Text(
            fileName,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.constrainAs(fileNameRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(fileIcon.end, 14.dp)
                end.linkTo(checkBox.start, 14.dp)
                width = Dimension.fillToConstraints
            })
        Image(
            painter = painterResource(id = if (isSelected) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
            "",
            Modifier
                .size(32.dp)
                .constrainAs(checkBox) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
                .clickable {
                    onSelect()
                }
        )
    }
}
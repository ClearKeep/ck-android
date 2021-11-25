package com.clearkeep.presentation.screen.chat.room.filepicker

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.ButtonType
import com.clearkeep.common.presentation.components.base.CKButton
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.common.presentation.components.separatorDarkNonOpaque
import com.clearkeep.presentation.screen.chat.room.RoomViewModel
import com.clearkeep.utilities.files.getFileName
import com.clearkeep.common.utilities.sdp

@ExperimentalMaterialApi
@Composable
fun FilePickerBottomSheetDialog(roomViewModel: RoomViewModel, onClickNext: () -> Unit) {
    val context = LocalContext.current
    val addFileContract = object : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<out String>): Intent {
            return super.createIntent(context, input)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }
    val addFileLauncher =
        rememberLauncherForActivityResult(addFileContract) {
            if (it != null) {
                roomViewModel.addStagedFileUri(it)
            }
        }
    val videoMime = "video/*"
    val applicationMime = "application/*"
    val audioMime = "audio/*"
    val textMime = "text/*"
    val fontMime = "font/*"
    val mimeTypes = arrayOf(videoMime, applicationMime, audioMime, textMime, fontMime)
    val stagedFiles = roomViewModel.fileUriStaged.observeAsState()

    Column(Modifier.padding(horizontal = 12.sdp())) {
        Spacer(Modifier.height(42.sdp()))
        Row(
            Modifier
                .fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth()) {
                CKText(stringResource(R.string.file_picker_your_file), Modifier.align(Alignment.CenterStart), color = LocalColorMapping.current.bodyTextAlt)
                Row(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .clickable {
                            addFileLauncher.launch(mimeTypes)
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.ic_plus), null, colorFilter = LocalColorMapping.current.closeIconFilter)
                    CKText(stringResource(R.string.file_picker_add), color = LocalColorMapping.current.bodyTextAlt)
                }
            }
        }
        Spacer(Modifier.height(32.sdp()))
        if (!stagedFiles.value.isNullOrEmpty()) {
            LazyColumn {
                itemsIndexed(stagedFiles.value!!.entries.toList()) { _: Int, entry: Map.Entry<Uri, Boolean> ->
                    FilePickerItem(
                        Modifier.padding(vertical = 16.sdp()),
                        entry.key.getFileName(context),
                        isSelected = stagedFiles.value!![entry.key] ?: false
                    ) {
                        roomViewModel.toggleSelectedFile(entry.key)
                    }
                    Divider(Modifier.height(1.sdp()), separatorDarkNonOpaque)
                }
                item {
                    Spacer(Modifier.height(18.sdp()))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Spacer(Modifier.height(18.sdp()))
                        CKButton(
                            label = stringResource(R.string.btn_next),
                            onClick = { onClickNext() },
                            Modifier
                                .height(52.sdp())
                                .width(210.sdp())
                                .clip(RoundedCornerShape(26.sdp())),
                            buttonType = if (LocalColorMapping.current.isDarkTheme) ButtonType.Blue else ButtonType.Black
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(18.sdp()))
            CKButton(
                label = stringResource(R.string.btn_next),
                onClick = { onClickNext() },
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(52.sdp())
                    .width(210.sdp())
                    .clip(RoundedCornerShape(26.sdp())),
                buttonType = if (LocalColorMapping.current.isDarkTheme) ButtonType.Blue else ButtonType.Black
            )
        }
        Spacer(Modifier.height(41.sdp()))
    }
}

@Composable
fun FilePickerItem(
    modifier: Modifier = Modifier,
    fileName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    ConstraintLayout(modifier.then(Modifier.fillMaxWidth())) {
        val (fileIcon, fileNameRef, checkBox) = createRefs()
        Image(painterResource(R.drawable.ic_file), null,
            Modifier
                .size(45.sdp())
                .constrainAs(fileIcon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                })
        CKText(
            fileName,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.constrainAs(fileNameRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(fileIcon.end, 14.dp)
                end.linkTo(checkBox.start, 14.dp)
                width = Dimension.fillToConstraints
            },
            color = LocalColorMapping.current.bodyTextAlt
        )
        Image(
            painter = painterResource(id = if (isSelected) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
            "",
            Modifier
                .size(32.sdp())
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
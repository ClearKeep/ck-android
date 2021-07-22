package com.clearkeep.screen.chat.room.composes

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.grayscale4
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.files.getFileName
import com.clearkeep.utilities.files.getFileSize
import com.clearkeep.utilities.printlnCK

@Composable
fun FileMessageContent(fileUrls: List<String>, onClick: (uri: String) -> Unit) {
    Column {
        fileUrls.forEach {
            MessageFileItem(it, onClick)
        }
    }
}

@Composable
fun MessageFileItem(fileUrl: String, onClick: (uri: String) -> Unit) {
    val context = LocalContext.current
    val fileName = if (isTempFile(fileUrl)) Uri.parse(fileUrl).getFileName(context) else getFileNameFromUrl(fileUrl)
    val fileSize = if (isTempFile(fileUrl)) Uri.parse(fileUrl).getFileSize(context) else getFileSizeInBytesFromUrl(fileUrl)
    val clickableModifier = if (!isTempFile(fileUrl)) Modifier.clickable { onClick.invoke(fileUrl) } else Modifier

    Column(
        Modifier
            .padding(28.dp, 8.dp, 54.dp, 8.dp)
            .then(clickableModifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_file_download), null, Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(fileName, color = grayscaleOffWhite)
        }
        Text(getFileSizeInMegabytesString(fileSize), color = grayscale4, fontSize = 12.sp)
    }
}

private fun isTempFile(uri: String) = uri.startsWith("content://")

private fun getFileNameFromUrl(url: String): String {
    val fileNameRegex = "(?:.(?!\\/))+\$".toRegex()
    val fileName = fileNameRegex.find(url)?.value?.replace(fileSizeRegex, "")
    return fileName?.substring(1 until fileName.length) ?: ""
}

private fun getFileSizeInBytesFromUrl(url: String): Long {
    val fileSizeString = fileSizeRegex.find(url)?.value
    return fileSizeString?.substring(1 until fileSizeString.length)?.toLongOrNull() ?: 0L
}

private fun getFileSizeInMegabytesString(fileSizeInBytes: Long): String {
    val fileSizeInMegabytes = fileSizeInBytes.toDouble() / 1_000_000
    return "%.2f MB".format(fileSizeInMegabytes)
}

private val fileSizeRegex = "\\|.+".toRegex()
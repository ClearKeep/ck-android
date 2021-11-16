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
import com.clearkeep.R
import com.clearkeep.presentation.components.grayscale2
import com.clearkeep.presentation.components.grayscale4
import com.clearkeep.presentation.components.grayscaleOffWhite
import com.clearkeep.utilities.*
import com.clearkeep.utilities.files.getFileName
import com.clearkeep.utilities.files.getFileSize

@Composable
fun FileMessageContent(fileUrls: List<String>, isQuote: Boolean, onClick: (uri: String) -> Unit) {
    Column {
        fileUrls.forEach {
            MessageFileItem(it, isQuote, onClick)
        }
    }
}

@Composable
fun MessageFileItem(fileUrl: String, isQuote: Boolean, onClick: (uri: String) -> Unit) {
    val context = LocalContext.current
    val fileName =
        if (isTempFile(fileUrl)) Uri.parse(fileUrl).getFileName(context) else getFileNameFromUrl(
            fileUrl
        )
    val fileSize = if (isTempFile(fileUrl)) Uri.parse(fileUrl)
        .getFileSize(context) else getFileSizeInBytesFromUrl(fileUrl)
    val clickableModifier =
        if (!isTempFile(fileUrl)) Modifier.clickable { onClick.invoke(fileUrl) } else Modifier

    Column(
        Modifier
            .padding(28.sdp(), 8.sdp(), 54.sdp(), 8.sdp())
            .then(clickableModifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_file_download), null, Modifier.size(20.sdp()))
            Spacer(Modifier.width(12.sdp()))
            Text(fileName, color = grayscaleOffWhite)
        }
        Text(
            getFileSizeInMegabytesString(fileSize),
            color = if (isQuote) grayscale2 else grayscale4,
            fontSize = 12.sdp().toNonScalableTextSize()
        )
    }
}

private fun isTempFile(uri: String) = uri.startsWith("content://")

private fun getFileSizeInBytesFromUrl(url: String): Long {
    val fileSizeString = fileSizeRegex.find(url)?.value
    return fileSizeString?.substring(1 until fileSizeString.length)?.toLongOrNull() ?: 0L
}

private fun getFileSizeInMegabytesString(fileSizeInBytes: Long): String {
    val unit = when {
        fileSizeInBytes < 1024 -> "B"
        fileSizeInBytes < 1024 * 1_000 -> "kB"
        else -> "MB"
    }
    val fileSizeInMegabytes = when {
        fileSizeInBytes < 1024 -> fileSizeInBytes.toDouble()
        fileSizeInBytes < 1024 * 1_000 -> fileSizeInBytes.toDouble() / 1_000
        else -> fileSizeInBytes.toDouble() / 1_000_000
    }
    return if (fileSizeInBytes < 1024) "$fileSizeInBytes $unit" else "%.2f $unit".format(
        fileSizeInMegabytes
    )
}
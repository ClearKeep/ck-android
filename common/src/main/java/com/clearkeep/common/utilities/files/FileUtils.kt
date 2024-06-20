package com.clearkeep.common.utilities.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

fun generatePhotoUri(context: Context): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context.applicationContext,
        context.packageName + ".provider",
        file
    )
}

fun getFileMimeType(context: Context, uri: Uri, persistablePermission: Boolean = true): String {
    val contentResolver = context.contentResolver
    if (persistablePermission) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val mimeType = contentResolver.getType(uri)
    return mimeType ?: ""
}

fun byteArrayToMd5HashString(byteArray: ByteArray): String {
    val bigInt = BigInteger(1, byteArray)
    val hashString = bigInt.toString(16)
    return String.format("%32s", hashString).replace(' ', '0')
}
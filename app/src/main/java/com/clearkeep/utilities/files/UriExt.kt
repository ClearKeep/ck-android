package com.clearkeep.utilities.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

fun Uri.getFileName(context: Context, persistablePermission: Boolean = true): String {
    val contentResolver = context.contentResolver
    if (persistablePermission) {
        try {
            contentResolver.takePersistableUriPermission(this, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {

        }
    }
    val cursor = contentResolver.query(this, null, null, null, null, null)
    if (cursor != null && cursor.moveToFirst()) {
        val fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        cursor.close()
        return fileName
    }
    cursor?.close()
    return ""
}

fun Uri.getFileSize(context: Context, persistablePermission: Boolean = true): Long {
    val contentResolver = context.contentResolver
    if (persistablePermission) {
        try {
            contentResolver.takePersistableUriPermission(this, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {

        }
    }
    val cursor = contentResolver.query(this, null, null, null, null, null)
    if (cursor != null && cursor.moveToFirst()) {
        val fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
        cursor.close()
        return fileSize
    }
    return 0L
}
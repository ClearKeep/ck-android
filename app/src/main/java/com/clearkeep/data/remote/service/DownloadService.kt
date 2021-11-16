package com.clearkeep.data.remote.service

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DownloadService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun downloadFile(fileName: String, url: String) {
        val dmRequest = DownloadManager.Request(Uri.parse(url))
        dmRequest.setDescription("Downloading file")
        dmRequest.setTitle(fileName)
        dmRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        dmRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val manager = (context.getSystemService(Context.DOWNLOAD_SERVICE)) as DownloadManager
        manager.enqueue(dmRequest)
    }
}
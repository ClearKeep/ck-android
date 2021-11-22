package com.clearkeep.domain.repository

import android.content.Context
import com.clearkeep.utilities.network.Resource
import upload_file.UploadFileOuterClass

interface FileRepository {
    suspend fun uploadFile(
        context: Context,
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String>

    suspend fun getUploadedFileUrl(
        fileName: String,
        mimeType: String
    ): UploadFileOuterClass.GetUploadFileLinkResponse

    fun downloadFile(fileName: String, url: String)
}
package com.clearkeep.domain.repository

import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.response.GetUploadFileLinkResponse

interface FileRepository {
    suspend fun uploadFile(
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String>

    suspend fun getUploadedFileUrl(
        fileName: String,
        mimeType: String
    ): GetUploadFileLinkResponse

    fun downloadFile(fileName: String, url: String)
}
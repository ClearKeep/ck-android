package com.clearkeep.data.remote.service

import com.clearkeep.data.remote.dynamicapi.DynamicAPIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import upload_file.UploadFileOuterClass
import javax.inject.Inject

class UploadFileService @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
) {
    suspend fun getUploadFileUrl(fileName: String, mimeType: String): UploadFileOuterClass.GetUploadFileLinkResponse = withContext(Dispatchers.Main) {
        val request = UploadFileOuterClass.GetUploadFileLinkRequest.newBuilder()
            .setFileName(fileName)
            .setFileContentType(mimeType)
            .setIsPublic(true)
            .build()

        return@withContext dynamicAPIProvider.provideUploadFileBlockingStub()
            .getUploadFileLink(request)
    }
}
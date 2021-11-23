package com.clearkeep.domain.usecase.chat

import android.content.Context
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.FileRepository
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(private val fileRepository: FileRepository) {
    suspend operator fun invoke(
        mimeType: String,
        fileName: String,
        fileUri: String
    ) = fileRepository.uploadFile(mimeType, fileName, fileUri)
}
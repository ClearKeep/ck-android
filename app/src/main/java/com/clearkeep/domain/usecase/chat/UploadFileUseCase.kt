package com.clearkeep.domain.usecase.chat

import android.content.Context
import com.clearkeep.domain.repository.ChatRepository
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(
        context: Context,
        mimeType: String,
        fileName: String,
        fileUri: String
    ) = chatRepository.uploadFile(context, mimeType, fileName, fileUri)
}
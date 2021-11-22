package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.FileRepository
import com.clearkeep.utilities.fileSizeRegex
import com.clearkeep.utilities.getFileNameFromUrl
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(private val fileRepository: FileRepository) {
    operator fun invoke(rawUrl: String) {
        val fileName = getFileNameFromUrl(rawUrl)
        val fileUrl = getFileUrl(rawUrl)
        fileRepository.downloadFile(fileName, fileUrl)
    }

    private fun getFileUrl(urlWithFileSize: String): String {
        return urlWithFileSize.replace(fileSizeRegex, "")
    }
}
package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class ClearTempMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke() = messageRepository.clearTempMessage()
}
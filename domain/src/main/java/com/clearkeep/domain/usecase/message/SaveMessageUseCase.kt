package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Message
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(message: com.clearkeep.domain.model.Message) = messageRepository.saveMessage(message)
}
package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Message
import com.clearkeep.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(message: Message) = withContext(Dispatchers.IO) { messageRepository.saveMessage(message) }
}
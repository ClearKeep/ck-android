package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetUnreadMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
        groupId: Long,
        domain: String,
        ourClientId: String
    ) = messageRepository.getUnreadMessage(groupId, domain, ourClientId)
}
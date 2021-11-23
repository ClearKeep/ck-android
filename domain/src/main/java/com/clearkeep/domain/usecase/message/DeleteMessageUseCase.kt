package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(domain: String, userId: String) =
        messageRepository.deleteMessageByDomain(domain, userId)

    suspend operator fun invoke(groupId: Long, ownerDomain: String, ownerClientId: String) =
        messageRepository.deleteMessageInGroup(groupId, ownerDomain, ownerClientId)
}
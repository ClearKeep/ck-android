package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class UpdateMessageFromApiUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
        groupId: Long,
        owner: Owner,
        lastMessageAt: Long = 0,
        loadSize: Int = 20
    ) = messageRepository.updateMessageFromAPI(groupId, owner, lastMessageAt, loadSize)
}
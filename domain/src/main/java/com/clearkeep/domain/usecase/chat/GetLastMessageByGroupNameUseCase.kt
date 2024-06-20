package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetLastMessageByGroupNameUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    suspend fun invoke(groupId: Long) = messageRepository.getLastMessageByGroupName(groupId)
}
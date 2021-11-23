package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetMessageAsStateUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    operator fun invoke(groupId: Long, owner: com.clearkeep.domain.model.Owner) = messageRepository.getMessagesAsState(groupId, owner)
}
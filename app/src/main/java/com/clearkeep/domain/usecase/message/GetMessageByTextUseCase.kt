package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetMessageByTextUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    operator fun invoke(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ) = messageRepository.getMessageByText(ownerDomain, ownerClientId, query)
}
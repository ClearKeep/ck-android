package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetNotesAsStateUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    operator fun invoke(owner: Owner) = messageRepository.getNotesAsState(owner)
}
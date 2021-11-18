package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class UpdateNotesFromApiUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(owner: Owner) = messageRepository.updateNotesFromAPI(owner)
}
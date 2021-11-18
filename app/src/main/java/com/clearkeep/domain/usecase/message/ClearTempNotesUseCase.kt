package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class ClearTempNotesUseCase @Inject constructor(private val messegeRepository: MessageRepository) {
    suspend operator fun invoke() = messegeRepository.clearTempNotes()
}
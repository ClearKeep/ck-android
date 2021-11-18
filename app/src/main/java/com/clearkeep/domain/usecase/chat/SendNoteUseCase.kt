package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.model.Note
import com.clearkeep.domain.repository.ChatRepository
import javax.inject.Inject

class SendNoteUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(note: Note, cachedNoteId: Long = 0) =
        chatRepository.sendNote(note, cachedNoteId)
}
package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Note
import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(note: Note) = messageRepository.saveNote(note)
}
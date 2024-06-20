package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class SetJoiningRoomUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    operator fun invoke(roomId: Long) = messageRepository.setJoiningRoomId(roomId)
}
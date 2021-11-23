package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.ChatRepository
import javax.inject.Inject

class SetJoiningRoomUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(roomId: Long) = chatRepository.setJoiningRoomId(roomId)
}
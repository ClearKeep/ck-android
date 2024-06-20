package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.MessageRepository
import javax.inject.Inject

class GetJoiningRoomUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    operator fun invoke() = messageRepository.getJoiningRoomId()
}
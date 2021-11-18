package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.ChatRepository
import javax.inject.Inject

class GetJoiningRoomUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke() = chatRepository.getJoiningRoomId()
}
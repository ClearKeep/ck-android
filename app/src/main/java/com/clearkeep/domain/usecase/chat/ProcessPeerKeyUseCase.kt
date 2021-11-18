package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.repository.ChatRepository
import javax.inject.Inject

class ProcessPeerKeyUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ) = chatRepository.processPeerKey(receiverId, receiverWorkspaceDomain, senderId, ownerWorkSpace)
}
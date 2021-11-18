package com.clearkeep.domain.usecase.chat

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(private val chatRepository: ChatRepository, private val serverRepository: ServerRepository) {
    suspend fun toPeer(
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean = false,
        cachedMessageId: Int = 0
    ): Resource<Nothing> {
        val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId)) //TODO: CLEAN ARCH Move logic to UseCase
        if (server == null) {
            printlnCK("sendMessageInPeer: server must be not null")
            return Resource.error("", null)
        }

        return chatRepository.sendMessageInPeer(
            server,
            senderId,
            ownerWorkSpace,
            receiverId,
            receiverWorkspaceDomain,
            groupId,
            plainMessage,
            isForceProcessKey,
            cachedMessageId
        )
    }

    suspend fun toGroup(
        senderId: String,
        ownerWorkSpace: String,
        groupId: Long,
        plainMessage: String,
        cachedMessageId: Int = 0
    ) = chatRepository.sendMessageToGroup(
        senderId,
        ownerWorkSpace,
        groupId,
        plainMessage,
        cachedMessageId
    )
}
package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.MessageRepository
import com.google.protobuf.ByteString
import javax.inject.Inject

class DecryptMessageUseCase @Inject constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
        messageId: String,
        groupId: Long,
        groupType: String,
        fromClientId: String,
        fromDomain: String,
        createdTime: Long,
        updatedTime: Long,
        encryptedMessage: ByteString,
        owner: Owner,
    ) = messageRepository.decryptMessage(
        messageId,
        groupId,
        groupType,
        fromClientId,
        fromDomain,
        createdTime,
        updatedTime,
        encryptedMessage,
        owner
    )
}
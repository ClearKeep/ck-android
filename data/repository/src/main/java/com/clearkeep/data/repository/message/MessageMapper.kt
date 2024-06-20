package com.clearkeep.data.repository.message

import com.clearkeep.data.local.clearkeep.message.MessageEntity
import com.clearkeep.domain.model.Message

fun Message.toEntity() = MessageEntity(
    generateId,
    messageId,
    groupId,
    groupType,
    senderId,
    receiverId,
    message,
    createdTime,
    updatedTime,
    ownerDomain,
    ownerClientId
)

fun MessageEntity.toModel() = Message(
    generateId,
    messageId,
    groupId,
    groupType,
    senderId,
    receiverId,
    message,
    createdTime,
    updatedTime,
    ownerDomain,
    ownerClientId
)
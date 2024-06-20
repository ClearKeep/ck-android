package com.clearkeep.data.repository.group

import com.clearkeep.data.local.clearkeep.group.ChatGroupEntity
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.response.ClientReadObject
import com.clearkeep.domain.model.response.GetMessagesInGroupResponse
import com.clearkeep.domain.model.response.GroupObjectResponse
import com.clearkeep.domain.model.response.MessageObjectResponse
import group.GroupOuterClass
import message.MessageOuterClass
import com.clearkeep.data.repository.message.toEntity
import com.clearkeep.data.repository.message.toModel

fun ChatGroup.toEntity() = ChatGroupEntity(
    generateId, groupId,
    groupName,
    groupAvatar,
    groupType,
    createBy,
    createdAt,
    updateBy,
    updateAt,
    rtcToken,
    clientList,
    isJoined,
    ownerDomain,
    ownerClientId,
    lastMessage?.toEntity(),
    lastMessageAt,
    lastMessageSyncTimestamp,
    isDeletedUserPeer
)

fun ChatGroupEntity.toModel() = ChatGroup(
    generateId,
    groupId,
    groupName,
    groupAvatar,
    groupType,
    createBy,
    createdAt,
    updateBy,
    updateAt,
    rtcToken,
    clientList,
    isJoined,
    ownerDomain,
    ownerClientId,
    lastMessage?.toModel(),
    lastMessageAt,
    lastMessageSyncTimestamp,
    isDeletedUserPeer
)

fun MessageOuterClass.ClientReadObject.toEntity() = ClientReadObject(id, displayName, avatar)

fun MessageOuterClass.MessageObjectResponse.toEntity() = MessageObjectResponse(
    id,
    groupId,
    groupType,
    fromClientId,
    fromClientWorkspaceDomain,
    clientId,
    clientWorkspaceDomain,
    message.toByteArray(),
    lstClientReadList.map { it.toEntity() },
    createdAt,
    updatedAt,
    senderMessage.toByteArray()
)

fun MessageOuterClass.GetMessagesInGroupResponse.toEntity() =
    GetMessagesInGroupResponse(lstMessageList.map { it.toEntity() })

fun GroupOuterClass.GroupObjectResponse.toEntity() = GroupObjectResponse(
    groupId,
    groupName,
    groupAvatar,
    groupType,
    lstClientList.map {
        GroupObjectResponse.ClientInGroupResponse(
            it.id,
            it.displayName,
            it.workspaceDomain,
            it.status
        )
    },
    lastMessageAt,
    lastMessage = GroupObjectResponse.MessageObjectResponse(
        lastMessage.id,
        lastMessage.groupId,
        lastMessage.groupType,
        lastMessage.fromClientId,
        lastMessage.clientId,
        lastMessage.message.toByteArray(),
        lastMessage.lstClientReadList.map {
            ClientReadObject(
                it.id,
                it.displayName,
                it.avatar
            )
        }
    ),
    createdByClientId,
    createdAt,
    updatedByClientId,
    updatedAt,
    groupRtcToken,
    hasUnreadMessage,
    clientKey = GroupObjectResponse.GroupClientKeyObject(
        clientKey.workspaceDomain,
        clientKey.clientId,
        clientKey.deviceId,
        clientKey.clientKeyDistribution.toByteArray(),
        clientKey.senderKeyId,
        clientKey.senderKey.toByteArray(),
        clientKey.publicKey.toByteArray(),
        clientKey.privateKey
    )
)
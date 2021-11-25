package com.clearkeep.domain.model

data class MessageObjectResponse(
    val id: String,
    val groupId: Long,
    val groupType: String,
    val fromClientId: String,
    val fromClientWorkspaceDomain: String,
    val clientId: String,
    val clientWorkspaceDomain: String,
    val message: ByteArray,
    val lstClientRead: List<ClientReadObject>,
    val createdAt: Long,
    val updatedAt: Long,
    val senderMessage: ByteArray
)

data class GetMessagesInGroupResponse(val lstMessage: List<MessageObjectResponse>)
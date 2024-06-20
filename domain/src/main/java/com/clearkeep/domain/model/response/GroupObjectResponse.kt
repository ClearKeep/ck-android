package com.clearkeep.domain.model.response

data class GroupObjectResponse(
    val groupId: Long,
    val groupName: String,
    val groupAvatar: String,
    val groupType: String,
    val lstClient: List<ClientInGroupResponse>,
    val lastMessageAt: Long,
    val lastMessage: MessageObjectResponse,
    val createdByClientId: String,
    val createdAt: Long,
    val updatedByClientId: String,
    val updatedAt: Long,
    val groupRtcToken: String,
    val hasUnreadMessage: Boolean,
    val clientKey: GroupClientKeyObject
) {
    data class GroupClientKeyObject(
        val workspaceDomain: String,
        val clientId: String,
        val deviceId: Int,
        val clientKeyDistribution: ByteArray,
        val senderKeyId: Long,
        val senderKey: ByteArray,
        val publicKey: ByteArray,
        val privateKey: String
    )

    data class ClientInGroupResponse(
        val id: String,
        val displayName: String,
        val workspaceDomain: String,
        val status: String
    )

    data class MessageObjectResponse(
        val id: String,
        val groupId: Long,
        val groupType: String,
        val fromClientId: String,
        val clientId:String,
        val message: ByteArray,
        val lstClientRead: List<ClientReadObject>
    )
}

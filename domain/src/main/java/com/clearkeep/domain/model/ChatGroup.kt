package com.clearkeep.domain.model

import com.clearkeep.common.utilities.isGroup

const val GROUP_ID_TEMPO = (-1).toLong()

data class ChatGroup(
    val generateId: Int? = null,
    val groupId: Long,
    val groupName: String,
    val groupAvatar: String?,
    val groupType: String,
    val createBy: String,
    val createdAt: Long,
    val updateBy: String,
    val updateAt: Long,
    val rtcToken: String,
    var clientList: List<User>,
    val isJoined: Boolean = false,
    val ownerDomain: String,
    val ownerClientId: String,
    var lastMessage: Message?,
    val lastMessageAt: Long,
    val lastMessageSyncTimestamp: Long,
    val isDeletedUserPeer: Boolean
) {
    fun isGroup() = isGroup(groupType)

    fun isGroupTempo() = GROUP_ID_TEMPO != groupId

    override fun toString(): String {
        return "groupID = $groupId, groupName = $groupName, groupType = $groupType, isJoined = $isJoined, clientList = $clientList, isDeletedUserPeer = $isDeletedUserPeer"
    }

    val owner: Owner
        get() = Owner(ownerDomain, ownerClientId)
}
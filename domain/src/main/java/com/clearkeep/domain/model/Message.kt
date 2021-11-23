package com.clearkeep.domain.model

import com.clearkeep.common.utilities.isGroup

data class Message(
    val generateId: Int? = null,
    val messageId: String,
    val groupId: Long,
    val groupType: String,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val createdTime: Long,
    val updatedTime: Long,
    val ownerDomain: String,
    val ownerClientId: String,
) {
    val owner: Owner
        get() = Owner(ownerDomain, ownerClientId)

    fun isGroupMessage() = isGroup(groupType)
}
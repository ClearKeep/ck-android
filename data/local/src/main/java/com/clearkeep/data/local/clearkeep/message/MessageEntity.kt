package com.clearkeep.data.local.clearkeep.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearkeep.common.utilities.isGroup
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.Owner

@Entity(tableName = "Message")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "group_type") val groupType: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "receiver_id") val receiverId: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "created_time") val createdTime: Long,
    @ColumnInfo(name = "updated_time") val updatedTime: Long,
    @ColumnInfo(name = "owner_domain") val ownerDomain: String,
    @ColumnInfo(name = "owner_client_id") val ownerClientId: String,
) {
    val owner: Owner
        get() = Owner(ownerDomain, ownerClientId)

    fun isGroupMessage() = isGroup(groupType)
}


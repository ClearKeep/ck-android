package com.clearkeep.db.clear_keep.model

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.screen.chat.utils.isGroup

@Entity
data class Message(
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

    fun toMessageDisplayInfo(isOwner: Boolean, showAvatarAndName: Boolean, showSpacer: Boolean, userName: String, cornerShape: RoundedCornerShape, avatar: String): MessageDisplayInfo {
        return MessageDisplayInfo(this, isOwner, showAvatarAndName, showSpacer, userName, cornerShape, avatar)
    }
}
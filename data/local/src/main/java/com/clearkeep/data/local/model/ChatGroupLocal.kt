package com.clearkeep.data.local.model

import androidx.annotation.Nullable
import androidx.room.*
import com.clearkeep.common.utilities.isGroup
import com.clearkeep.data.local.clearkeep.converter.MessageConverter
import com.clearkeep.data.local.clearkeep.converter.PeopleListConverter
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User

const val GROUP_ID_TEMPO = (-1).toLong()

@Entity(tableName = "ChatGroup")
@TypeConverters(MessageConverter::class, PeopleListConverter::class)
data class ChatGroupLocal(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "group_name") val groupName: String,
    @ColumnInfo(name = "group_avatar") val groupAvatar: String?,
    @ColumnInfo(name = "group_type") val groupType: String,
    @ColumnInfo(name = "created_by_client_id") val createBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_by_client_id") val updateBy: String,
    @ColumnInfo(name = "updated_at") val updateAt: Long,
    @ColumnInfo(name = "group_rtc_token") val rtcToken: String,

    @ColumnInfo(name = "lst_client") var clientList: List<User>,

    @ColumnInfo(name = "is_registered_to_group") val isJoined: Boolean = false,

    @ColumnInfo(name = "owner_domain") val ownerDomain: String,
    @ColumnInfo(name = "owner_client_id") val ownerClientId: String,

    @Nullable
    @ColumnInfo(name = "last_message") val lastMessage: MessageLocal?,

    @ColumnInfo(name = "last_message_at") val lastMessageAt: Long,

    @ColumnInfo(name = "last_message_sync_stamp") val lastMessageSyncTimestamp: Long,
    @ColumnInfo(name = "is_deleted_user_peer") val isDeletedUserPeer: Boolean
) {
    fun isGroup() = isGroup(groupType)

    fun isGroupTempo() = GROUP_ID_TEMPO != groupId

    override fun toString(): String {
        return "groupName = $groupName, groupType = $groupType, isJoined = $isJoined, clientList = $clientList, isDeletedUserPeer = $isDeletedUserPeer"
    }

    val owner: Owner
        get() = Owner(ownerDomain, ownerClientId)

    fun toEntity() = ChatGroup(
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
        lastMessage?.toEntity(),
        lastMessageAt,
        lastMessageSyncTimestamp,
        isDeletedUserPeer
    )
}

fun ChatGroup.toLocal() = ChatGroupLocal(
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
    lastMessage?.toLocal(),
    lastMessageAt,
    lastMessageSyncTimestamp,
    isDeletedUserPeer
)
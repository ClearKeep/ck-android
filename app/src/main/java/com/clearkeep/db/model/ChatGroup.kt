package com.clearkeep.db.model

import androidx.annotation.NonNull
import androidx.room.*
import com.clearkeep.db.converter.SortedStringListConverter
import com.clearkeep.screen.chat.utils.isGroup

const val GROUP_ID_TEMPO = "id_not_create"

@Entity
@TypeConverters(SortedStringListConverter::class)
data class ChatGroup(
        @NonNull
        @PrimaryKey val id: String,
        @ColumnInfo(name = "group_name") val groupName: String,
        @ColumnInfo(name = "group_avatar") val groupAvatar: String?,
        @ColumnInfo(name = "group_type") val groupType: String,
        @ColumnInfo(name = "created_by_client_id") val createBy: String,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "updated_by_client_id") val updateBy: String,
        @ColumnInfo(name = "updated_at") val updateAt: Long,

        @ColumnInfo(name = "lst_client_id") val clientList: List<String>,

        @ColumnInfo(name = "is_registered_to_group") val isJoined: Boolean = false,

        @ColumnInfo(name = "last_client") val lastClient: String,
        @ColumnInfo(name = "last_message") val lastMessage: String,
        @ColumnInfo(name = "last_updated_time") val lastUpdatedTime: Long,
) {
        fun isGroup() = isGroup(groupType)

        fun isGroupTempo() = GROUP_ID_TEMPO != id
}
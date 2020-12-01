package com.clearkeep.db.model

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.*
import com.clearkeep.db.converter.MessageConverter
import com.clearkeep.db.converter.PeopleListConverter
import com.clearkeep.db.converter.SortedStringListConverter
import com.clearkeep.screen.chat.utils.isGroup

const val GROUP_ID_TEMPO = "id_not_create"

@Entity
@TypeConverters(SortedStringListConverter::class, MessageConverter::class, PeopleListConverter::class)
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

        @ColumnInfo(name = "lst_client") val clientList: List<People>,

        @ColumnInfo(name = "is_registered_to_group") val isJoined: Boolean = false,

        @Nullable
        @ColumnInfo(name = "last_message") val lastMessage: Message?,

        @ColumnInfo(name = "last_message_at") val lastMessageAt: Long,
) {
        fun isGroup() = isGroup(groupType)

        fun isGroupTempo() = GROUP_ID_TEMPO != id
}
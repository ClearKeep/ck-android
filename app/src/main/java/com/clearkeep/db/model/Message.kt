package com.clearkeep.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
        @ColumnInfo(name = "sender_id") val senderId: String,
        @ColumnInfo(name = "message") val message: String,
        @ColumnInfo(name = "group_id") val groupId: String,
        @ColumnInfo(name = "created_time") val createdTime: Long,
        @ColumnInfo(name = "updated_time") val updatedTime: Long,
        @ColumnInfo(name = "receiver_id") val receiverId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
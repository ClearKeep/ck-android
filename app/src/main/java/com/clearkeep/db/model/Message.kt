package com.clearkeep.db.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @NonNull
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "group_type") val groupType: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "receiver_id") val receiverId: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "created_time") val createdTime: Long,
    @ColumnInfo(name = "updated_time") val updatedTime: Long,
)
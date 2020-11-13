package com.clearkeep.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Room(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        @ColumnInfo(name = "room_name") val roomName: String,
        @ColumnInfo(name = "remote_id") val remoteId: String,
        @ColumnInfo(name = "is_group") val isGroup: Boolean,
        @ColumnInfo(name = "is_accepted") val isAccepted: Boolean = false,

        @ColumnInfo(name = "last_people") val lastPeople: String,
        @ColumnInfo(name = "last_message") val lastMessage: String,
        @ColumnInfo(name = "last_updated_time") val lastUpdatedTime: Long,
        @ColumnInfo(name = "is_read") val isRead: Boolean
)
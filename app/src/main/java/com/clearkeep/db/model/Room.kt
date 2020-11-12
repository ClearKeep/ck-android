package com.clearkeep.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Room(
    @ColumnInfo(name = "room_name") val roomName: String,
    @ColumnInfo(name = "remote_id") val remoteId: String,
    @ColumnInfo(name = "is_group") val isGroup: Boolean,
    @ColumnInfo(name = "is_accepted") val isAccepted: Boolean = false,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
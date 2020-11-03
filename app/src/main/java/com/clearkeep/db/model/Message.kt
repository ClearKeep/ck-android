package com.clearkeep.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
        @ColumnInfo(name = "sender_id") val senderId: String,
        @ColumnInfo(name = "message") val message: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
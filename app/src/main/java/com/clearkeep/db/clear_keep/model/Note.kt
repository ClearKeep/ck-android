package com.clearkeep.db.clear_keep.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note (
    @PrimaryKey(autoGenerate = true) val generateId: Long? = null,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "created_time") val createdTime: Long,
    @ColumnInfo(name = "owner_domain") val ownerDomain: String,
    @ColumnInfo(name = "owner_client_id") val ownerClientId: String,
)
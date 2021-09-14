package com.clearkeep.db.clear_keep.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["server_domain", "user_id"])
data class UserKey(
    @ColumnInfo(name = "server_domain")
    val serverDomain: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "k")
    val key: String,
    @ColumnInfo(name = "salt")
    val salt: String
)
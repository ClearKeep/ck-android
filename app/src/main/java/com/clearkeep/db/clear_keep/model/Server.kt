package com.clearkeep.db.clear_keep.model

import androidx.annotation.NonNull
import androidx.room.*

@Entity
data class Server(
        @NonNull
        @PrimaryKey val id: Long,
        @ColumnInfo(name = "server_name") val serverName: String,
        @ColumnInfo(name = "server_avatar") val serverAvatar: String?,
)
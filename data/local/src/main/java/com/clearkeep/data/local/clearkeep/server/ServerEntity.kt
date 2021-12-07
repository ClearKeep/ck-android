package com.clearkeep.data.local.clearkeep.server

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Server")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "server_name") val serverName: String,
    @ColumnInfo(name = "server_domain") val serverDomain: String,
    @ColumnInfo(name = "owner_client_id") val ownerClientId: String,
    @ColumnInfo(name = "server_avatar") val serverAvatar: String?,
    @ColumnInfo(name = "time_user_login") val loginTime: Long,
    @ColumnInfo(name = "access_token") val accessKey: String,
    @ColumnInfo(name = "hash_key") val hashKey: String,
    @ColumnInfo(name = "refresh_token") val refreshToken: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "owner") val profile: ProfileEntity,
) {
    override fun toString(): String {
        return "${serverName}, $serverDomain, $ownerClientId   \n profile.userId: ${profile.userId}"
    }
}
package com.clearkeep.db.clear_keep.model

import androidx.room.*
import com.clearkeep.db.clear_keep.converter.ProfileConverter

@Entity
@TypeConverters(ProfileConverter::class)
data class Server(
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
    @ColumnInfo(name = "owner") val profile: Profile,
) {
        override fun toString(): String {
                return "${serverName}, $serverDomain, $accessKey, $hashKey"
        }
}
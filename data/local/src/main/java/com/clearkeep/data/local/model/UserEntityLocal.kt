package com.clearkeep.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearkeep.domain.model.UserEntity
import com.clearkeep.domain.model.UserStatus

@Entity(tableName = "UserEntity")
data class UserEntityLocal(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val userName: String,
    @ColumnInfo(name = "domain") val domain: String,

    @ColumnInfo(name = "owner_client_id") val ownerClientId: String,
    @ColumnInfo(name = "owner_domain") val ownerDomain: String,
    @ColumnInfo(name = "user_status") var userStatus: String? = UserStatus.ONLINE.value,
    @ColumnInfo(name = "phone_number") var phoneNumber: String? = "",
    @ColumnInfo(name = "avatar") var avatar: String? = "",
    @ColumnInfo(name = "email") var email: String? = "",
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain"
    }

    fun toEntity() = UserEntity(
        generateId,
        userId,
        userName,
        domain,
        ownerClientId,
        ownerDomain,
        userStatus,
        phoneNumber,
        avatar,
        email
    )
}

fun UserEntity.toLocal() = UserEntityLocal(
    generateId,
    userId,
    userName,
    domain,
    ownerClientId,
    ownerDomain,
    userStatus,
    phoneNumber,
    avatar,
    email
)
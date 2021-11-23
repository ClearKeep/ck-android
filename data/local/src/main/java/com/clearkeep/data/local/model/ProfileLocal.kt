package com.clearkeep.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearkeep.domain.model.Profile

@Entity(tableName = "Profile")
data class ProfileLocal(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "user_name") val userName: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone_number") val phoneNumber: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "avatar") var avatar: String?,
) {
    fun toEntity() = Profile(
        generateId,
        userId,
        userName,
        email,
        phoneNumber,
        updatedAt,
        avatar
    )
}

fun Profile.toLocal() =
    ProfileLocal(generateId, userId, userName, email, phoneNumber, updatedAt, avatar)
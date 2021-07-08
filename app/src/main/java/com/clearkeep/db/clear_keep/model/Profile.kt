package com.clearkeep.db.clear_keep.model

import androidx.room.*

@Entity
data class Profile(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "user_name") val userName: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?,
) {
    fun getDisplayName() : String {
        return userName ?: (firstName ?: "" + (lastName ?: ""))
    }
}
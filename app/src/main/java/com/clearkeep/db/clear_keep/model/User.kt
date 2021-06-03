package com.clearkeep.db.clear_keep.model

import androidx.annotation.NonNull
import androidx.room.*

@Entity
data class User(
    @NonNull
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_name") val userName: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?,
) {
    fun getDisplayName() : String {
        return userName ?: (firstName ?: "" + (lastName ?: ""))
    }
}
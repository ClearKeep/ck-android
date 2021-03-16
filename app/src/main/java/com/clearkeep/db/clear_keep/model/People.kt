package com.clearkeep.db.clear_keep.model

import androidx.annotation.NonNull
import androidx.room.*

@Entity
data class People(
    @NonNull
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_name") val userName: String,
) {
    override fun toString(): String {
        return "userName = $userName"
    }
}
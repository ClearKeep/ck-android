package com.clearkeep.db.clear_keep.model

import androidx.room.*

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val generateId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "owner_domain") val ownerDomain: String,
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $ownerDomain"
    }
}
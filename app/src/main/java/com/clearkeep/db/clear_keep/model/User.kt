package com.clearkeep.db.clear_keep.model

import androidx.room.*
import java.util.*

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

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is User) {
            return false
        }
        return userId == other.userId && ownerDomain == other.ownerDomain
    }

    override fun hashCode(): Int {
        return Objects.hash(userId, userName, ownerDomain)
    }
}
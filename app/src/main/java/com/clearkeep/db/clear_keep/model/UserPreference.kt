package com.clearkeep.db.clear_keep.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["server_domain", "user_id"])
data class UserPreference(
    @ColumnInfo(name = "server_domain")
    val serverDomain: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "show_notification_preview")
    val showNotificationPreview: Boolean,
    @ColumnInfo(name = "do_not_disturb")
    val doNotDisturb: Boolean,
    @ColumnInfo(name = "mfa")
    val mfa: Boolean,
    @ColumnInfo(name = "is_social_account")
    val isSocialAccount: Boolean
) {
    companion object {
        fun getDefaultUserPreference(serverDomain: String, userId: String, isSocialAccount: Boolean) = UserPreference(
            serverDomain,
            userId,
            showNotificationPreview = true,
            doNotDisturb = false,
            mfa = false,
            isSocialAccount = isSocialAccount
        )
    }
}
package com.clearkeep.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.clearkeep.domain.model.UserPreference

@Entity(tableName = "UserPreference", primaryKeys = ["server_domain", "user_id"])
data class UserPreferenceLocal(
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
    fun toEntity() = UserPreference(
        serverDomain,
        userId,
        showNotificationPreview,
        doNotDisturb,
        mfa,
        isSocialAccount
    )
}

fun UserPreference.toLocal() = UserPreferenceLocal(
    serverDomain,
    userId,
    showNotificationPreview,
    doNotDisturb,
    mfa,
    isSocialAccount
)
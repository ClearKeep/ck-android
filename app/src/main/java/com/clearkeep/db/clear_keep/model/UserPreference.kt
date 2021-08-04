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
    val doNotDisturb: Boolean
)
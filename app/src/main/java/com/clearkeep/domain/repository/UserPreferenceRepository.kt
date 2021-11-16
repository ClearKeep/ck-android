package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.UserPreference

interface UserPreferenceRepository {
    suspend fun initDefaultUserPreference(
        serverDomain: String,
        userId: String,
        isSocialAccount: Boolean = false
    )

    fun getUserPreferenceState(serverDomain: String, userId: String): LiveData<UserPreference>
    suspend fun getUserPreference(serverDomain: String, userId: String): UserPreference?
    suspend fun updateShowNotificationPreview(
        serverDomain: String,
        userId: String,
        enabled: Boolean
    )

    suspend fun updateDoNotDisturb(serverDomain: String, userId: String, enabled: Boolean)
    suspend fun updateMfa(serverDomain: String, userId: String, enabled: Boolean)
}
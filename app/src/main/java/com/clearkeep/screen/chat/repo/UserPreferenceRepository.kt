package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.dao.UserPreferenceDAO
import com.clearkeep.db.clear_keep.model.UserPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferenceRepository @Inject constructor(
    private val userPreferenceDAO: UserPreferenceDAO
) {
    suspend fun initDefaultUserPreference(serverDomain: String, userId: String) {
        val defaultSettings = UserPreference(
            serverDomain,
            userId,
            showNotificationPreview = true,
            notificationSoundVibrate = true,
            doNotDisturb = false
        )
        userPreferenceDAO.insert(defaultSettings)
    }

    suspend fun getUserPreference(serverDomain: String, userId: String) {
        userPreferenceDAO.getPreference(serverDomain, userId)
    }

    suspend fun updateShowNotificationPreview(
        serverDomain: String,
        userId: String,
        enabled: Boolean
    ) {
        userPreferenceDAO.updateDoNotDisturb(serverDomain, userId, enabled)
    }

    suspend fun updateNotificationSoundVibrate(
        serverDomain: String,
        userId: String,
        enabled: Boolean
    ) {
        userPreferenceDAO.updateNotificationSoundVibrate(serverDomain, userId, enabled)
    }

    suspend fun updateDoNotDisturb(serverDomain: String, userId: String, enabled: Boolean) {
        userPreferenceDAO.updateDoNotDisturb(serverDomain, userId, enabled)
    }
}
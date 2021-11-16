package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserPreferenceDAO
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.repository.UserPreferenceRepository
import javax.inject.Inject

class UserPreferenceRepositoryImpl @Inject constructor(
    private val userPreferenceDAO: UserPreferenceDAO
) : UserPreferenceRepository {
    override suspend fun initDefaultUserPreference(
        serverDomain: String,
        userId: String,
        isSocialAccount: Boolean
    ) {
        val defaultSettings =
            UserPreference.getDefaultUserPreference(serverDomain, userId, isSocialAccount)
        userPreferenceDAO.insert(defaultSettings)
    }

    override fun getUserPreferenceState(serverDomain: String, userId: String) =
        userPreferenceDAO.getPreferenceLiveData(serverDomain, userId)

    override suspend fun getUserPreference(serverDomain: String, userId: String) =
        userPreferenceDAO.getPreference(serverDomain, userId)

    override suspend fun updateShowNotificationPreview(
        serverDomain: String,
        userId: String,
        enabled: Boolean
    ) {
        userPreferenceDAO.updateNotificationPreview(serverDomain, userId, enabled)
    }

    override suspend fun updateDoNotDisturb(serverDomain: String, userId: String, enabled: Boolean) {
        userPreferenceDAO.updateDoNotDisturb(serverDomain, userId, enabled)
    }

    override suspend fun updateMfa(serverDomain: String, userId: String, enabled: Boolean) {
        userPreferenceDAO.updateMfa(serverDomain, userId, enabled)
    }
}
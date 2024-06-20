package com.clearkeep.domain.usecase.preferences

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.repository.UserPreferenceRepository
import javax.inject.Inject

class GetUserPreferenceUseCase @Inject constructor(private val userPreferenceRepository: UserPreferenceRepository) {
    suspend operator fun invoke(
        serverDomain: String,
        userId: String
    ): UserPreference? {
        return userPreferenceRepository.getUserPreference(serverDomain, userId)
    }

    fun asState(
        serverDomain: String,
        userId: String,
    ): LiveData<UserPreference> {
        return userPreferenceRepository.getUserPreferenceState(serverDomain, userId)
    }
}
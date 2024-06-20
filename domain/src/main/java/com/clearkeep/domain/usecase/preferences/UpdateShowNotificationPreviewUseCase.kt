package com.clearkeep.domain.usecase.preferences

import com.clearkeep.domain.repository.UserPreferenceRepository
import javax.inject.Inject

class UpdateShowNotificationPreviewUseCase @Inject constructor(private val userPreferenceRepository: UserPreferenceRepository) {
    suspend operator fun invoke(serverDomain: String, userId: String, enabled: Boolean) =
        userPreferenceRepository.updateShowNotificationPreview(serverDomain, userId, enabled)
}
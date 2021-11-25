package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(owner: Owner, profile: Profile): Boolean {
        val validatedProfile = profile.copy(
            phoneNumber = profile.phoneNumber?.trim(),
            userName = profile.userName?.trim()
        )

        val server = serverRepository.getServerByOwner(owner) ?: return false

        val updateProfileSuccess = profileRepository.updateProfile(server, validatedProfile)
        if (updateProfileSuccess) {
            serverRepository.updateServerProfile(owner.domain, validatedProfile)
        }

        return updateProfileSuccess
    }
}
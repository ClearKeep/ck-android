package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class GetMfaSettingsUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(owner: com.clearkeep.domain.model.Owner) {
        val server = serverRepository.getServerByOwner(owner) ?: return
        profileRepository.getMfaSettings(server)
    }
}
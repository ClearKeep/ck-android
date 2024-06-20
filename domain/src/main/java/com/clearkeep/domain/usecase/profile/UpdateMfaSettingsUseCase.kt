package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import javax.inject.Inject

class UpdateMfaSettingsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(owner: Owner, enabled: Boolean): Resource<Pair<String, String>> {
        val server =
            serverRepository.getServerByOwner(owner)
                ?: return Resource.error("", null)

        return profileRepository.updateMfaSettings(server, enabled)
    }
}
package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class MfaResendOtpUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(owner: Owner): Resource<Pair<Int, String>> {
        val server = serverRepository.getServerByOwner(owner) ?: return Resource.error("", 0 to "")

        return profileRepository.mfaResendOtp(server)
    }
}
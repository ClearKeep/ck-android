package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(owner: Owner, email: String, oldPassword: String, newPassword: String): Resource<String> {
        val server = serverRepository.getServerByOwner(owner) ?: return Resource.error("", null)

        return profileRepository.changePassword(server, email, oldPassword, newPassword)
    }
}
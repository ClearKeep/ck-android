package com.clearkeep.domain.usecase.notification

import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class RegisterTokenUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(token: String) {
        val server = serverRepository.getServers()
        profileRepository.registerToken(token, server)
    }
}
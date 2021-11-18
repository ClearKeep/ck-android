package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(server: Server) = authRepository.logoutFromAPI(server)
}
package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class LoginByGoogleUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        token: String,
        domain: String
    ) = authRepository.loginByGoogle(token, domain)
}
package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class LoginByFacebookUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        token: String,
        domain: String
    ) = authRepository.loginByFacebook(token, domain)
}
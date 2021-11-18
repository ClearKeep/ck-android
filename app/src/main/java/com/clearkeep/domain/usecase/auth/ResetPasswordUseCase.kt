package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        preAccessToken: String,
        email: String,
        domain: String,
        rawNewPassword: String
    ) = authRepository.resetPassword(preAccessToken, email, domain, rawNewPassword)
}
package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterSocialPinUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        domain: String,
        rawPin: String,
        userName: String
    ) = authRepository.registerSocialPin(domain, rawPin, userName)
}
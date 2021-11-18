package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class VerifySocialPinUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        domain: String,
        rawPin: String,
        userName: String
    ) = authRepository.verifySocialPin(domain, rawPin, userName)
}
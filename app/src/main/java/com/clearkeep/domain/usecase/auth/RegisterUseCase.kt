package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.presentation.screen.videojanus.dismissInCallNotification
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ) = authRepository.register(displayName.trim(), password, email.trim(), domain)
}
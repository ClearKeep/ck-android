package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(userName: String, password: String, domain: String) =
        authRepository.login(userName.trim(), password, domain)
}
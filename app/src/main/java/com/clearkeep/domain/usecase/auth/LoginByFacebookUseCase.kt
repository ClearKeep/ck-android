package com.clearkeep.domain.usecase.auth

import auth.AuthOuterClass
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class LoginByFacebookUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        token: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes> = authRepository.loginByFacebook(token, domain)
}
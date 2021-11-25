package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.response.SocialLoginRes
import javax.inject.Inject

class LoginByFacebookUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        token: String,
        domain: String
    ): Resource<SocialLoginRes> = authRepository.loginByFacebook(token, domain)
}
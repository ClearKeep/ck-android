package com.clearkeep.domain.usecase.auth

import auth.AuthOuterClass
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class ValidateOtpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthOuterClass.AuthRes> = authRepository.validateOtp(domain, otp, otpHash, userId, hashKey)
}
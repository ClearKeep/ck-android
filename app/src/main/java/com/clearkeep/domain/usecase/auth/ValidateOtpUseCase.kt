package com.clearkeep.domain.usecase.auth

import auth.AuthOuterClass
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.UserPreferenceRepository
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import java.lang.RuntimeException
import javax.inject.Inject

class ValidateOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthOuterClass.AuthRes> {
        val validateOtpResponse = authRepository.validateOtp(domain, otp, otpHash, userId, hashKey)
        if (validateOtpResponse.status == Status.SUCCESS) {
            val accessToken = validateOtpResponse.data?.accessToken ?: ""
            val profile = authRepository.getProfile(domain, accessToken, hashKey)
                ?: return Resource.error("Can not get profile", null)
            serverRepository.insertServer(
                Server(
                    serverName = validateOtpResponse.data?.workspaceName ?: "",
                    serverDomain = domain,
                    ownerClientId = profile.userId,
                    serverAvatar = "",
                    loginTime = getCurrentDateTime().time,
                    accessKey = accessToken,
                    hashKey = hashKey,
                    refreshToken = validateOtpResponse.data?.refreshToken ?: "",
                    profile = profile,
                )
            )
            userPreferenceRepository.initDefaultUserPreference(
                domain,
                profile.userId,
                isSocialAccount = false
            )
        }
        return validateOtpResponse
    }
}
package com.clearkeep.domain.usecase.auth

import com.clearkeep.common.utilities.getCurrentDateTime
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.UserPreferenceRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.domain.model.response.AuthRes
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
    ): Resource<AuthRes> {
        val validateOtpResponse = authRepository.validateOtp(domain, otp, otpHash, userId, hashKey)
        if (validateOtpResponse.status == Status.SUCCESS) {
            val accessToken = validateOtpResponse.data?.accessToken ?: ""
            val profile = authRepository.getProfile(domain, accessToken, hashKey)
                ?: return Resource.error("Can not get profile", null)
            serverRepository.insertServer(
                com.clearkeep.domain.model.Server(
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
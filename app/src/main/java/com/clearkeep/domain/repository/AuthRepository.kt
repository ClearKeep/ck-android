package com.clearkeep.domain.repository

import auth.AuthOuterClass
import com.clearkeep.domain.model.LoginResponse
import com.clearkeep.domain.model.Server
import com.clearkeep.utilities.network.Resource

interface AuthRepository {
    suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<AuthOuterClass.RegisterSRPRes>
    suspend fun login(userName: String, password: String, domain: String): Resource<LoginResponse>
    suspend fun loginByGoogle(
        token: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes>
    suspend fun loginByFacebook(
        token: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes>
    suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes>
    suspend fun registerSocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthOuterClass.AuthRes>
    suspend fun verifySocialPin(domain: String, rawPin: String, userName: String): Resource<AuthOuterClass.AuthRes>
    suspend fun resetSocialPin(
        domain: String,
        rawPin: String,
        userName: String,
        resetPincodeToken: String
    ): Resource<AuthOuterClass.AuthRes>
    suspend fun resetPassword(
        preAccessToken: String,
        email: String,
        domain: String,
        rawNewPassword: String
    ): Resource<AuthOuterClass.AuthRes>
    suspend fun recoverPassword(
        email: String,
        domain: String
    ): Resource<AuthOuterClass.BaseResponse>
    suspend fun logoutFromAPI(server: Server): Resource<AuthOuterClass.BaseResponse>
    suspend fun validateOtp(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthOuterClass.AuthRes>
    suspend fun mfaResendOtp(
        domain: String,
        otpHash: String,
        userId: String
    ): Resource<Pair<Int, String>>
}
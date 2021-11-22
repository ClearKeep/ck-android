package com.clearkeep.domain.repository

import auth.AuthOuterClass
import com.clearkeep.domain.model.LoginResponse
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.utilities.network.Resource

interface AuthRepository {
    suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<AuthOuterClass.RegisterSRPRes>

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
        transitionID: Int,
        identityKeyPublic: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        userName: String,
        saltHex: String,
        verificatorHex: String,
        iv: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes>

    suspend fun verifySocialPin(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes>

    suspend fun resetSocialPin(
        transitionID: Int,
        publicKey: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKey: ByteArray,
        signedPreKeyId: Int,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        userName: String,
        resetPincodeToken: String,
        verficatorHex: String,
        saltHex: String,
        iv: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes>

    suspend fun resetPassword(
        transitionID: Int,
        publicKey: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        preAccessToken: String,
        email: String,
        verificatorHex: String,
        saltHex: String,
        iv: String,
        domain: String
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

    suspend fun getProfile(domain: String, accessToken: String, hashKey: String): Profile?

    suspend fun sendLoginChallenge(
        username: String,
        aHex: String,
        domain: String
    ): Resource<AuthOuterClass.AuthChallengeRes>

    suspend fun sendLoginSocialChallenge(
        userName: String,
        aHex: String,
        domain: String
    ): Resource<AuthOuterClass.AuthChallengeRes>

    suspend fun loginAuthenticate(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes>
}
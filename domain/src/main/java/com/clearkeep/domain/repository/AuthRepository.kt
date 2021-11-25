package com.clearkeep.domain.repository

import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.response.AuthChallengeRes
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.model.response.SocialLoginRes

interface AuthRepository {
    suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<Any>

    suspend fun loginByGoogle(
        token: String,
        domain: String
    ): Resource<SocialLoginRes>

    suspend fun loginByFacebook(
        token: String,
        domain: String
    ): Resource<SocialLoginRes>

    suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<SocialLoginRes>

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
    ): Resource<AuthRes>

    suspend fun verifySocialPin(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthRes>

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
    ): Resource<AuthRes>

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
    ): Resource<AuthRes>

    suspend fun recoverPassword(
        email: String,
        domain: String
    ): Resource<String>

    suspend fun logoutFromAPI(server: Server): Resource<String>

    suspend fun validateOtp(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthRes>

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
    ): Resource<AuthChallengeRes>

    suspend fun sendLoginSocialChallenge(
        userName: String,
        aHex: String,
        domain: String
    ): Resource<AuthChallengeRes>

    suspend fun loginAuthenticate(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthRes>
}
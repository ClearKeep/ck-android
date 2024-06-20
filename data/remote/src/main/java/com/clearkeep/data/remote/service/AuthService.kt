package com.clearkeep.data.remote.service

import auth.AuthOuterClass
import com.clearkeep.data.remote.dynamicapi.CallCredentials
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.clearkeep.common.utilities.REQUEST_DEADLINE_SECONDS
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthService @Inject constructor(
    private val paramAPIProvider: ParamAPIProvider
) {
    suspend fun registerSrp(
        domain: String,
        email: String,
        verificatorHex: String,
        saltHex: String,
        displayName: String,
        iv: String,
        transitionID: Int,
        identityKeyPublic: ByteArray,
        preKeyId: Int,
        preKey: ByteArray,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray
    ): AuthOuterClass.RegisterSRPRes = withContext(Dispatchers.IO) {
        val setClientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
            .setDeviceId(SENDER_DEVICE_ID)
            .setRegistrationId(transitionID)
            .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPublic))
            .setPreKey(ByteString.copyFrom(preKey))
            .setPreKeyId(preKeyId)
            .setSignedPreKeyId(signedPreKeyId)
            .setSignedPreKey(ByteString.copyFrom(signedPreKey))
            .setIdentityKeyEncrypted(identityKeyEncrypted)
            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKeySignature))
            .build()

        val request = AuthOuterClass.RegisterSRPReq.newBuilder()
            .setWorkspaceDomain(domain)
            .setEmail(email)
            .setPasswordVerifier(verificatorHex)
            .setSalt(saltHex)
            .setDisplayName(displayName)
            .setAuthType(0L)
            .setFirstName("")
            .setLastName("")
            .setClientKeyPeer(setClientKeyPeer)
            .setIvParameter(iv)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).registerSrp(request)
    }

    suspend fun loginChallenge(
        email: String,
        aHex: String,
        domain: String
    ): AuthOuterClass.AuthChallengeRes = withContext(Dispatchers.IO) {
        val request = AuthOuterClass.AuthChallengeReq.newBuilder()
            .setEmail(email)
            .setClientPublic(aHex)
            .build()
        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .loginChallenge(request)
    }

    suspend fun loginAuthenticate(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val authReq = AuthOuterClass.AuthenticateReq.newBuilder()
            .setUserName(userName)
            .setClientPublic(aHex)
            .setClientSessionKeyProof(mHex)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .loginAuthenticate(authReq)
    }

    suspend fun loginByGoogle(token: String, domain: String): AuthOuterClass.SocialLoginRes =
        withContext(Dispatchers.IO) {
            val request = AuthOuterClass
                .GoogleLoginReq
                .newBuilder()
                .setIdToken(token)
                .setWorkspaceDomain(domain)
                .build()
            return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
                .withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).loginGoogle(request)
        }

    suspend fun loginByFacebook(token: String, domain: String): AuthOuterClass.SocialLoginRes =
        withContext(Dispatchers.IO) {
            val request = AuthOuterClass
                .FacebookLoginReq
                .newBuilder()
                .setAccessToken(token)
                .setWorkspaceDomain(domain)
                .build()
            return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
                .withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).loginFacebook(request)
        }

    suspend fun loginByMicrosoft(token: String, domain: String): AuthOuterClass.SocialLoginRes =
        withContext(Dispatchers.IO) {
            val request = AuthOuterClass
                .OfficeLoginReq
                .newBuilder()
                .setAccessToken(token)
                .setWorkspaceDomain(domain)
                .build()
            return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
                .withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).loginOffice(request)
        }

    suspend fun registerPincode(
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
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val clientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
            .setDeviceId(SENDER_DEVICE_ID)
            .setRegistrationId(transitionID)
            .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPublic))
            .setPreKey(ByteString.copyFrom(preKey))
            .setPreKeyId(preKeyId)
            .setSignedPreKeyId(signedPreKeyId)
            .setSignedPreKey(
                ByteString.copyFrom(signedPreKey)
            )
            .setIdentityKeyEncrypted(
                identityKeyEncrypted
            )
            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKeySignature))
            .build()

        val request = AuthOuterClass
            .RegisterPinCodeReq
            .newBuilder()
            .setUserName(userName)
            .setHashPincode(verificatorHex)
            .setSalt(saltHex)
            .setClientKeyPeer(clientKeyPeer)
            .setIvParameter(iv)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).registerPincode(request)
    }

    suspend fun loginSocialChallenge(
        userName: String,
        aHex: String,
        domain: String
    ): AuthOuterClass.AuthChallengeRes = withContext(Dispatchers.IO) {
        val challengeReq = AuthOuterClass.AuthSocialChallengeReq
            .newBuilder()
            .setUserName(userName)
            .setClientPublic(aHex)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).loginSocialChallange(challengeReq)
    }

    suspend fun verifyPinCode(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val request = AuthOuterClass
            .VerifyPinCodeReq
            .newBuilder()
            .setUserName(userName)
            .setClientPublic(aHex)
            .setClientSessionKeyProof(mHex)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).verifyPincode(request)

    }

    suspend fun resetPinCode(
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
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val clientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
            .setDeviceId(SENDER_DEVICE_ID)
            .setRegistrationId(transitionID)
            .setIdentityKeyPublic(ByteString.copyFrom(publicKey))
            .setPreKey(ByteString.copyFrom(preKey))
            .setPreKeyId(preKeyId)
            .setSignedPreKeyId(signedPreKeyId)
            .setSignedPreKey(
                ByteString.copyFrom(signedPreKey)
            )
            .setIdentityKeyEncrypted(
                identityKeyEncrypted
            )
            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKeySignature))
            .build()

        val request = AuthOuterClass
            .ResetPinCodeReq
            .newBuilder()
            .setUserName(userName)
            .setResetPincodeToken(resetPincodeToken)
            .setHashPincode(verficatorHex)
            .setSalt(saltHex)
            .setIvParameter(iv)
            .setClientKeyPeer(clientKeyPeer)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).resetPincode(request)
    }

    suspend fun forgotPasswordUpdate(
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
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val clientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
            .setDeviceId(SENDER_DEVICE_ID)
            .setRegistrationId(transitionID)
            .setIdentityKeyPublic(ByteString.copyFrom(publicKey))
            .setPreKey(ByteString.copyFrom(preKey))
            .setPreKeyId(preKeyId)
            .setSignedPreKeyId(signedPreKeyId)
            .setSignedPreKey(ByteString.copyFrom(signedPreKey))
            .setIdentityKeyEncrypted(identityKeyEncrypted)
            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKeySignature))
            .build()

        val request = AuthOuterClass
            .ForgotPasswordUpdateReq
            .newBuilder()
            .setPreAccessToken(preAccessToken)
            .setEmail(email)
            .setPasswordVerifier(verificatorHex)
            .setSalt(saltHex)
            .setIvParameter(iv)
            .setClientKeyPeer(clientKeyPeer)
            .build()

        return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            .withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).forgotPasswordUpdate(request)
    }

    suspend fun forgotPassword(email: String, domain: String): AuthOuterClass.BaseResponse =
        withContext(Dispatchers.IO) {
            val request = AuthOuterClass.ForgotPasswordReq.newBuilder()
                .setEmail(email)
                .build()
            return@withContext paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
                .withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).forgotPassword(request)
        }

    suspend fun logout(deviceID: String, server: com.clearkeep.domain.model.Server): AuthOuterClass.BaseResponse =
        withContext(Dispatchers.IO) {
            val request = AuthOuterClass.LogoutReq.newBuilder()
                .setDeviceId(deviceID)
                .setRefreshToken(server.refreshToken)
                .build()

            val authBlockingWithHeader =
                paramAPIProvider.provideAuthBlockingStub(ParamAPI(server.serverDomain))
                    .withDeadlineAfter(10 * 1000, TimeUnit.MILLISECONDS)
                    .withCallCredentials(CallCredentials(server.accessKey, server.hashKey))

            return@withContext authBlockingWithHeader.logout(request)
        }

    suspend fun validateOtp(
        otp: String,
        otpHash: String,
        userId: String,
        domain: String
    ): AuthOuterClass.AuthRes = withContext(Dispatchers.IO) {
        val request = AuthOuterClass.MfaValidateOtpRequest.newBuilder()
            .setOtpCode(otp)
            .setPreAccessToken(otpHash)
            .setUserId(userId)
            .build()
        val stub = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
        return@withContext stub.validateOtp(request)
    }

    suspend fun resendOtp(
        otpHash: String,
        userId: String,
        domain: String
    ): AuthOuterClass.MfaResendOtpRes = withContext(Dispatchers.IO) {
        val request = AuthOuterClass.MfaResendOtpReq.newBuilder()
            .setPreAccessToken(otpHash)
            .setUserId(userId)
            .build()
        val stub = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
        return@withContext stub.resendOtp(request)
    }

    suspend fun getProfile(
        domain: String,
        accessToken: String,
        hashKey: String
    ): UserOuterClass.UserProfileResponse = withContext(Dispatchers.IO) {
        val userGrpc = paramAPIProvider.provideUserBlockingStub(
            ParamAPI(
                domain,
                accessToken,
                hashKey
            )
        )
        val request = UserOuterClass.Empty.newBuilder().build()
        return@withContext userGrpc.getProfile(request)
    }
}
package com.clearkeep.data.remote.service

import com.clearkeep.domain.model.Server
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import javax.inject.Inject

class UserService @Inject constructor(
    private val apiProvider: ParamAPIProvider
) {
    suspend fun getMfaSettings(server: com.clearkeep.domain.model.Server): UserOuterClass.MfaStateResponse =
        withContext(Dispatchers.IO) {
            val request = UserOuterClass.MfaGetStateRequest.newBuilder().build()

            return@withContext apiProvider.provideUserBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            ).getMfaState(request)
        }

    suspend fun updateMfaSettings(
        server: com.clearkeep.domain.model.Server,
        enabled: Boolean
    ): UserOuterClass.MfaBaseResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.MfaChangingStateRequest.newBuilder().build()
        val stub = apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        return@withContext if (enabled) {
            stub.enableMfa(request)
        } else {
            stub.disableMfa(request)
        }
    }

    suspend fun mfaAuthChallenge(
        server: com.clearkeep.domain.model.Server,
        aHex: String
    ): UserOuterClass.MfaAuthChallengeResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.MfaAuthChallengeRequest.newBuilder()
            .setClientPublic(aHex)
            .build()

        return@withContext apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
            .mfaAuthChallenge(request)
    }

    suspend fun mfaValidatePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String,
        mHex: String
    ): UserOuterClass.MfaBaseResponse = withContext(Dispatchers.IO) {
        val validateRequest = UserOuterClass.MfaValidatePasswordRequest.newBuilder()
            .setClientPublic(aHex)
            .setClientSessionKeyProof(mHex)
            .build()
        val stub = apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )

        return@withContext stub.mfaValidatePassword(validateRequest)
    }

    suspend fun mfaValidateOtp(server: com.clearkeep.domain.model.Server, otp: String): UserOuterClass.MfaBaseResponse =
        withContext(Dispatchers.IO) {
            val request = UserOuterClass.MfaValidateOtpRequest.newBuilder()
                .setOtp(otp)
                .build()
            val stub = apiProvider.provideUserBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            )

            return@withContext stub.mfaValidateOtp(request)
        }

    suspend fun mfaRequestResendOtp(server: com.clearkeep.domain.model.Server): UserOuterClass.MfaBaseResponse =
        withContext(Dispatchers.IO) {
            val request = UserOuterClass.MfaResendOtpRequest.newBuilder().build()
            val stub = apiProvider.provideUserBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            )

            return@withContext stub.mfaResendOtp(request)
        }

    suspend fun requestChangePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String
    ): UserOuterClass.RequestChangePasswordRes = withContext(Dispatchers.IO) {
        val request = UserOuterClass.RequestChangePasswordReq.newBuilder()
            .setClientPublic(aHex)
            .build()
        return@withContext apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        ).requestChangePassword(request)
    }

    suspend fun changePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String,
        mHex: String,
        verificatorHex: String,
        newSaltHex: String,
        ivParam: String,
        identityKeyEncrypted: String?
    ): UserOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val changePasswordRequest = UserOuterClass.ChangePasswordRequest.newBuilder()
            .setClientPublic(aHex)
            .setClientSessionKeyProof(mHex)
            .setHashPassword(verificatorHex)
            .setSalt(newSaltHex)
            .setIvParameter(ivParam)
            .setIdentityKeyEncrypted(identityKeyEncrypted)
            .build()
        val stub = apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        return@withContext stub.changePassword(changePasswordRequest)
    }

    suspend fun uploadAvatar(
        server: com.clearkeep.domain.model.Server,
        fileName: String,
        mimeType: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ): UserOuterClass.UploadAvatarResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.UploadAvatarRequest.newBuilder()
            .setFileName(fileName)
            .setFileContentType(mimeType)
            .setFileData(byteStrings[0])
            .setFileHash(fileHash)
            .build()

        return@withContext apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        ).uploadAvatar(request)
    }

    suspend fun updateProfile(
        server: com.clearkeep.domain.model.Server,
        phoneNumber: String?,
        displayName: String?,
        avatar: String?
    ): UserOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val requestBuilder = UserOuterClass.UpdateProfileRequest.newBuilder()
            .setDisplayName(displayName)
            .setPhoneNumber(phoneNumber)

        if (phoneNumber.isNullOrBlank()) {
            requestBuilder.clearPhoneNumber = true
        }
        if (avatar != null) requestBuilder.avatar = avatar
        val request = requestBuilder.build()
        val userGrpc = apiProvider.provideUserBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        return@withContext userGrpc.updateProfile(request)
    }
}
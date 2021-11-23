package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.common.utilities.network.Resource
import com.google.protobuf.ByteString
import user.UserOuterClass

interface ProfileRepository {
    suspend fun updateProfile(server: Server, profile: Profile): Boolean
    suspend fun uploadAvatar(
        server: Server,
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ): String

    suspend fun getMfaSettings(server: Server)
    suspend fun updateMfaSettings(server: Server, enabled: Boolean): Resource<Pair<String, String>>
    suspend fun mfaValidatePassword(
        server: Server,
        aHex: String,
        mHex: String,
    ): Resource<Pair<String, String>>

    suspend fun mfaValidateOtp(server: Server, owner: com.clearkeep.domain.model.Owner, otp: String): Resource<String>
    suspend fun mfaResendOtp(server: Server): Resource<Pair<Int, String>>
    suspend fun sendMfaAuthChallenge(
        server: Server,
        aHex: String
    ): UserOuterClass.MfaAuthChallengeResponse

    suspend fun requestChangePassword(server: Server, aHex: String): UserOuterClass.RequestChangePasswordRes
    suspend fun changePassword(
        server: Server,
        aHex: String,
        mHex: String,
        verificatorHex: String,
        newSaltHex: String,
        ivParam: String,
        identityKeyEncrypted: String?
    ): Resource<String>
}
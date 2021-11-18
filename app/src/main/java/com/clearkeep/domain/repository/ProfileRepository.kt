package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString

interface ProfileRepository {
    suspend fun registerToken(token: String, servers: List<Server>)
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
        password: String
    ): Resource<Pair<String, String>>

    suspend fun mfaValidateOtp(server: Server, owner: Owner, otp: String): Resource<String>
    suspend fun mfaResendOtp(server: Server): Resource<Pair<Int, String>>
    suspend fun changePassword(
        server: Server,
        email: String,
        oldPassword: String,
        newPassword: String
    ): Resource<String>
}
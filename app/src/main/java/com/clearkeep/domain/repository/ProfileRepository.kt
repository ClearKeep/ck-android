package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Profile
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString

interface ProfileRepository {
    suspend fun registerToken(token: String)
    suspend fun updateProfile(owner: Owner, profile: Profile): Boolean
    suspend fun uploadAvatar(
        owner: Owner,
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ): String

    suspend fun getMfaSettingsFromAPI(owner: Owner)
    suspend fun updateMfaSettings(owner: Owner, enabled: Boolean): Resource<Pair<String, String>>
    suspend fun mfaValidatePassword(
        owner: Owner,
        password: String
    ): Resource<Pair<String, String>>

    suspend fun mfaValidateOtp(owner: Owner, otp: String): Resource<String>
    suspend fun mfaResendOtp(owner: Owner): Resource<Pair<Int, String>>
    suspend fun changePassword(
        owner: Owner,
        email: String,
        oldPassword: String,
        newPassword: String
    ): Resource<String>
}
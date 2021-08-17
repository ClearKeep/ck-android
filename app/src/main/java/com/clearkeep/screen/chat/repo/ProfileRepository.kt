package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.AppStorage
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import notify_push.NotifyPushOuterClass
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    // network calls
    private val apiProvider: ParamAPIProvider,

    // data
    private val serverRepository: ServerRepository,
    private val userPreferenceRepository: UserPreferenceRepository,

    private val userManager: AppStorage
) {
    suspend fun registerToken(token: String)  = withContext(Dispatchers.IO) {
        printlnCK("registerToken: token = $token")
        val server = serverRepository.getServers()
        server?.forEach { server ->
            registerTokenByOwner(token, server)
        }
    }

    private suspend fun registerTokenByOwner(token: String, server: Server) : Boolean  = withContext(Dispatchers.IO) {
        val deviceId = userManager.getUniqueDeviceID()
        printlnCK("registerTokenByOwner: domain = ${server.serverDomain}, clientId = ${server.profile.userId}, token = $token, deviceId = $deviceId")
        try {
            val request = NotifyPushOuterClass.RegisterTokenRequest.newBuilder()
                .setDeviceId(deviceId)
                .setDeviceType("android")
                .setToken(token)
                .build()
            val notifyPushGrpc = apiProvider.provideNotifyPushBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            )
            val response = notifyPushGrpc.registerToken(request)
            printlnCK("registerTokenByOwner success: domain = ${server.serverDomain}, clientId = ${server.profile.userId}")
            return@withContext response.success
        } catch (e: Exception) {
            printlnCK("registerTokenByOwner error: domain = ${server.serverDomain}, clientId = ${server.profile.userId}, $e")
            return@withContext false
        }
    }

    suspend fun updateProfile(
        owner: Owner,
        profile: Profile
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext false
            val requestBuilder = UserOuterClass.UpdateProfileRequest.newBuilder()
                .setDisplayName(profile.userName?.trim())
                .setPhoneNumber(profile.phoneNumber?.trim())
            if (profile.avatar != null) requestBuilder.avatar = profile.avatar
            val request = requestBuilder.build()
            val userGrpc = apiProvider.provideUserBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            )
            val response = userGrpc.updateProfile(request)
            printlnCK("updateProfile success? ${response.success} errors? ${response.errors}")
            if (response.success) {
                serverRepository.updateServerProfile(owner.domain, profile)
            }
            return@withContext response.success
        } catch (e: Exception) {
            printlnCK("updateProfile error: $e")
            return@withContext false
        }
    }

    suspend fun uploadAvatar(
        owner: Owner,
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ) : String {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverRepository.getServerByOwner(owner) ?: return@withContext ""
                val request = UserOuterClass.UploadAvatarRequest.newBuilder()
                    .setFileName(fileName)
                    .setFileContentType(mimeType)
                    .setFileData(byteStrings[0])
                    .setFileHash(fileHash)
                    .build()

                val response =
                    apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey)).uploadAvatar(request)

                printlnCK("uploadAvatar response ${response.fileUrl}")
                return@withContext response.fileUrl
            } catch (e: Exception) {
                printlnCK("uploadAvatar $e")
                return@withContext ""
            }
        }
    }

    suspend fun getMfaSettingsFromAPI(owner: Owner) = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext
            val request = UserOuterClass.MfaGetStateRequest.newBuilder().build()

            val response = apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey)).getMfaState(request)
            val isMfaEnabled = response.mfaEnable
            userPreferenceRepository.updateMfa(server.serverDomain, server.profile.userId, isMfaEnabled)
            printlnCK("getMfaSettingsFromAPI MFA enabled? $isMfaEnabled")
        } catch (exception: Exception) {
            printlnCK("getMfaSettingsFromAPI: $exception")
        }
    }

    suspend fun updateMfaSettings(owner: Owner, enabled: Boolean) : Boolean = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext false
            val request = UserOuterClass.MfaChangingStateRequest.newBuilder().build()
            val stub = apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            val response = if (enabled) {
                stub.enableMfa(request)
            } else {
                stub.disableMfa(request)
            }
            if (response.success) {
                userPreferenceRepository.updateMfa(server.serverDomain, server.profile.userId, enabled)
            }
            printlnCK("updateMfaSettings MFA change to $enabled success? ${response.success}")
            return@withContext response.success
        } catch (exception: Exception) {
            printlnCK("updateMfaSettings: $exception")
            return@withContext false
        }
    }

    suspend fun mfaValidatePassword(owner: Owner, password: String) : Resource<String> = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext Resource.error("", null)
            val request = UserOuterClass.MfaValidatePasswordRequest.newBuilder()
                .setPassword(password)
                .build()
            val stub = apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            val response = stub.mfaValidatePassword(request)
            printlnCK("mfaValidatePassword success? ${response.success} error? ${response.errors.message} code ${response.errors.code}")
            return@withContext if (response.success) Resource.success(null) else Resource.error(response.errors.toString(), null)
        } catch (exception: Exception) {
            printlnCK("mfaValidatePassword: $exception")
            return@withContext Resource.error(exception.toString(), null)
        }
    }
}
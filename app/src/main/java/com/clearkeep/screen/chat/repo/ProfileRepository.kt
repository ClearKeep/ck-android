package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.AppStorage
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
        displayName: String,
        phoneNumber: String,
        avatarUrl: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext false
            val requestBuilder = UserOuterClass.UpdateProfileRequest.newBuilder()
                .setDisplayName(displayName)
                .setPhoneNumber(phoneNumber)
            if (avatarUrl != null) requestBuilder.avatar = avatarUrl
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
}
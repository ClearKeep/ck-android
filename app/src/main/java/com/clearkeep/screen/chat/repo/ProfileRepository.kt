package com.clearkeep.screen.chat.repo

import auth.AuthOuterClass
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.AppStorage
import com.clearkeep.utilities.DecryptsPBKDF2
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
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
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("registerTokenByOwner token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext false
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
            printlnCK("updateProfile $profile")
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
            printlnCK("updateProfile success? ${response.error.isNullOrEmpty()} errors? ${response.error}")
            if (response.error.isNullOrEmpty()) {
                serverRepository.updateServerProfile(owner.domain, profile)
            }
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("updateProfile token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext false
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
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("uploadAvatar token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
                return@withContext ""
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
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true)
                    printlnCK("getMfaSettingsFromAPI token expired")
                    parsedError.message
                }
                else -> parsedError.message
            }
        } catch (exception: Exception) {
            printlnCK("getMfaSettingsFromAPI: $exception")
        }
    }

    suspend fun updateMfaSettings(owner: Owner, enabled: Boolean) : Resource<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext Resource.error("", null)
            val request = UserOuterClass.MfaChangingStateRequest.newBuilder().build()
            val stub = apiProvider.provideUserBlockingStub(
                ParamAPI(
                    server.serverDomain,
                    server.accessKey,
                    server.hashKey
                )
            )
            val response = if (enabled) {
                stub.enableMfa(request)
            } else {
                stub.disableMfa(request)
            }
            printlnCK("updateMfaSettings MFA change to $enabled success? ${response.success}")
            if (response.success && !enabled) {
                userPreferenceRepository.updateMfa(
                    server.serverDomain,
                    server.profile.userId,
                    false
                )
            }
            return@withContext Resource.success("" to "")
        } catch (e: StatusRuntimeException) {

            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true)
                    printlnCK("updateMfaSettings token expired")
                    "" to parsedError.message
                }
                1069 -> "Account is locked" to "Your account has been locked out due to too many attempts. Please try again later!"
                else -> "" to parsedError.message
            }
            return@withContext Resource.error("", message)
        } catch (exception: Exception) {
            printlnCK("updateMfaSettings: $exception")
            return@withContext Resource.error("", "" to exception.toString())
        }
    }

    suspend fun mfaValidatePassword(owner: Owner, password: String) : Resource<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext Resource.error("", "" to "")
            val request = UserOuterClass.MfaValidatePasswordRequest.newBuilder()
                .setPassword(password)
                .build()
            val stub = apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            val response = stub.mfaValidatePassword(request)
            return@withContext if (response.success) Resource.success("" to "") else Resource.error("", "" to response.error.toString())
        } catch (exception: StatusRuntimeException) {
            printlnCK("mfaValidatePassword: $exception")

            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("mfaValidatePassword token expired")
                    serverRepository.isLogout.postValue(true)
                    "Error" to "Expired token"
                }
                1001 -> "Error" to "The password is incorrect. Try again"
                1069 -> "Warning" to "Your account has been locked out due to too many attempts. Please try again later!"
                else -> "Error" to parsedError.message
            }
            return@withContext Resource.error("", message)
        } catch (exception: Exception) {
            printlnCK("mfaValidatePassword: $exception")
            return@withContext Resource.error("", "" to exception.toString())
        }
    }

    suspend fun mfaValidateOtp(owner: Owner, otp: String) : Resource<String> = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext Resource.error("", null)
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
            val response = stub.mfaValidateOtp(request)
            printlnCK("mfaValidateOtp success? ${response.success} error? ${response.error} code ${response.error}")
            return@withContext if (response.success) {
                userPreferenceRepository.updateMfa(owner.domain, owner.clientId, true)
                Resource.success(null)
            } else {
                Resource.error(response.error, null)
            }
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("mfaValidateOtp token expired")
                    serverRepository.isLogout.postValue(true)
                    ""
                }
                1071 -> "Authentication failed. Please retry."
                1068, 1072 -> "Verification code has expired. Please request a new code and retry."
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (exception: Exception) {
            printlnCK("mfaValidateOtp: $exception")
            return@withContext Resource.error(exception.toString(), null)
        }
    }

    suspend fun mfaResendOtp(owner: Owner) : Resource<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext Resource.error("", 0 to "")
            val request = UserOuterClass.MfaResendOtpRequest.newBuilder().build()
            val stub = apiProvider.provideUserBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            val response = stub.mfaResendOtp(request)
            printlnCK("mfaResendOtp success? ${response.success} error? ${response.error} code ${response}")
            return@withContext if (response.success) Resource.success(null) else Resource.error("", 0 to response.error)
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("mfaResendOtp token expired")
                    serverRepository.isLogout.postValue(true)
                    ""
                }
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> parsedError.message
            }
            return@withContext Resource.error("", parsedError.code to message)
        } catch (exception: Exception) {
            printlnCK("mfaResendOtp: $exception")
            return@withContext Resource.error("", 0 to exception.toString())
        }
    }

    suspend fun changePassword(owner: Owner, oldPassword: String, newPassword: String): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val server = serverRepository.getServerByOwner(owner)
                    ?: return@withContext Resource.error("", null)
                val request = UserOuterClass.ChangePasswordRequest.newBuilder()
                    .setOldPassword(DecryptsPBKDF2.md5(oldPassword))
                    .setNewPassword(DecryptsPBKDF2.md5(newPassword))
                    .build()
                val stub = apiProvider.provideUserBlockingStub(
                    ParamAPI(
                        server.serverDomain,
                        server.accessKey,
                        server.hashKey
                    )
                )
                val response = stub.changePassword(request)
                return@withContext if (response.error.isNullOrBlank()) Resource.success(null) else Resource.error(
                    "",
                    null
                )
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1001 -> {
                        "The password is incorrect. Try again"
                    }
                    1000, 1077 -> {
                        printlnCK("changePassword token expired")
                        serverRepository.isLogout.postValue(true)
                        ""
                    }
                    else -> parsedError.message
                }
                return@withContext Resource.error(message, null)
            } catch (exception: Exception) {
                printlnCK("changePassword: $exception")
                return@withContext Resource.error("", null)
            }
        }
}
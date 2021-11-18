package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserPreferenceDAO
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.data.remote.service.PushNotificationService
import com.clearkeep.data.remote.service.UserService
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.srp.NativeLib
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val serverRepository: ServerRepository, //TODO: Clean
    private val userManager: AppStorage,
    private val pushNotificationService: PushNotificationService,
    private val userPreferenceDAO: UserPreferenceDAO,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val userService: UserService
) : ProfileRepository {
    override suspend fun registerToken(token: String, servers: List<Server>) =
        withContext(Dispatchers.IO) {
            printlnCK("registerToken: token = $token")
            servers.forEach { server ->
                registerTokenByOwner(token, server)
            }
        }

    private suspend fun registerTokenByOwner(token: String, server: Server): Boolean =
        withContext(Dispatchers.IO) {
            val deviceId = userManager.getUniqueDeviceID()
            printlnCK("registerTokenByOwner: domain = ${server.serverDomain}, clientId = ${server.profile.userId}, token = $token, deviceId = $deviceId")
            try {
                val response =
                    pushNotificationService.registerPushNotificationToken(deviceId, token, server)
                printlnCK("registerTokenByOwner success: domain = ${server.serverDomain}, clientId = ${server.profile.userId}")
                return@withContext response.error.isNullOrEmpty()
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("registerTokenByOwner token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun updateProfile(
        server: Server,
        profile: Profile
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = userService.updateProfile(
                server,
                profile.phoneNumber,
                profile.userName,
                profile.avatar
            )
            return@withContext response.error.isNullOrEmpty()
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("updateProfile token expired")
                    serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun uploadAvatar(
        server: Server,
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response =
                    userService.uploadAvatar(server, fileName, mimeType, byteStrings, fileHash)
                return@withContext response.fileUrl
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("uploadAvatar token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun getMfaSettings(server: Server) = withContext(Dispatchers.IO) {
        try {
            val response = userService.getMfaSettings(server)
            val isMfaEnabled = response.mfaEnable
            userPreferenceDAO.updateMfa(
                server.serverDomain,
                server.profile.userId,
                isMfaEnabled
            )
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
                    printlnCK("getMfaSettingsFromAPI token expired")
                    parsedError.message
                }
                else -> parsedError.message
            }
        } catch (exception: Exception) {
            printlnCK("getMfaSettingsFromAPI: $exception")
        }
    }

    override suspend fun updateMfaSettings(
        server: Server,
        enabled: Boolean
    ): Resource<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.updateMfaSettings(server, enabled)
                printlnCK("updateMfaSettings MFA change to $enabled success? ${response.success}")
                if (response.success && !enabled) {
                    userPreferenceDAO.updateMfa(
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
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun mfaValidatePassword(
        server: Server,
        password: String
    ): Resource<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val nativeLib = NativeLib()
            val a = nativeLib.getA(server.profile.email ?: "", password)
            val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            val response = userService.mfaAuthChallenge(server, aHex)

            val salt = response.salt
            val b = response.publicChallengeB

            val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
            val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            nativeLib.freeMemoryAuthenticate()

            val validateResponse = userService.mfaValidatePassword(server, aHex, mHex)
            return@withContext if (validateResponse.success) Resource.success("" to "") else Resource.error(
                "",
                "" to response.toString()
            )
        } catch (exception: StatusRuntimeException) {
            printlnCK("mfaValidatePassword: $exception")

            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("mfaValidatePassword token expired")
                    serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun mfaValidateOtp(
        server: Server,
        owner: Owner,
        otp: String
    ): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.mfaValidateOtp(server, otp)
                printlnCK("mfaValidateOtp success? ${response.success} error? ${response.error} code ${response.error}")
                return@withContext if (response.success) {
                    userPreferenceDAO.updateMfa(owner.domain, owner.clientId, true)
                    Resource.success(null)
                } else {
                    Resource.error(response.error, null)
                }
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("mfaValidateOtp token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun mfaResendOtp(server: Server): Resource<Pair<Int, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.mfaRequestResendOtp(server)
                printlnCK("mfaResendOtp success? ${response.success} error? ${response.error} code $response")
                return@withContext if (response.success) Resource.success(null) else Resource.error(
                    "",
                    0 to response.error
                )
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("mfaResendOtp token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    override suspend fun changePassword(
        server: Server,
        email: String,
        oldPassword: String,
        newPassword: String
    ): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val nativeLib = NativeLib()
                val a = nativeLib.getA(email, oldPassword)

                val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val response = userService.requestChangePassword(server, aHex)

                val salt = response.salt
                val b = response.publicChallengeB

                val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
                val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
                nativeLib.freeMemoryAuthenticate()

                val newPasswordNativeLib = NativeLib()

                val newSalt = newPasswordNativeLib.getSalt(email, newPassword)
                val newSaltHex =
                    newSalt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val verificator = newPasswordNativeLib.getVerificator()
                val verificatorHex =
                    verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                nativeLib.freeMemoryCreateAccount()

                val decrypter = DecryptsPBKDF2(newPassword)

                val oldIdentityKey = signalIdentityKeyDAO.getIdentityKey(
                    server.profile.userId,
                    server.serverDomain
                )!!.identityKeyPair.privateKey.serialize()

                val decryptResult = decrypter.encrypt(
                    oldIdentityKey,
                    newSaltHex,
                )?.let {
                    toHex(
                        it
                    )
                }

                val changePasswordResponse = userService.changePassword(
                    server,
                    aHex,
                    mHex,
                    verificatorHex,
                    newSaltHex,
                    toHex(decrypter.getIv()),
                    decryptResult
                )
                return@withContext if (changePasswordResponse.error.isNullOrBlank()) Resource.success(
                    null
                ) else Resource.error(
                    "",
                    null
                )
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1001, 1079 -> {
                        "The password is incorrect. Try again"
                    }
                    1000, 1077 -> {
                        printlnCK("changePassword token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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
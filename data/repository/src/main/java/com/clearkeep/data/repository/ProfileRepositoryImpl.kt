package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.UserPreferenceDAO
import com.clearkeep.data.remote.service.UserService
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.utilities.*
import com.clearkeep.common.utilities.network.Resource
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import java.util.*
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userPreferenceDAO: UserPreferenceDAO,
    private val userService: UserService
) : ProfileRepository {
    override suspend fun updateProfile(
        server: com.clearkeep.domain.model.Server,
        profile: com.clearkeep.domain.model.Profile
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
            return@withContext false
        } catch (e: Exception) {
            printlnCK("updateProfile error: $e")
            return@withContext false
        }
    }

    override suspend fun uploadAvatar(
        server: com.clearkeep.domain.model.Server,
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
                return@withContext ""
            } catch (e: Exception) {
                printlnCK("uploadAvatar $e")
                return@withContext ""
            }
        }
    }

    override suspend fun getMfaSettings(server: com.clearkeep.domain.model.Server) = withContext(Dispatchers.IO) {
        try {
            val response = userService.getMfaSettings(server)
            val isMfaEnabled = response.mfaEnable
            userPreferenceDAO.updateMfa(
                server.serverDomain,
                server.profile.userId,
                isMfaEnabled
            )
        } catch (e: StatusRuntimeException) {
            printlnCK("getMfaSettingsFromAPI: $e")
        } catch (exception: Exception) {
            printlnCK("getMfaSettingsFromAPI: $exception")
        }
    }

    override suspend fun updateMfaSettings(
        server: com.clearkeep.domain.model.Server,
        enabled: Boolean
    ): com.clearkeep.common.utilities.network.Resource<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.updateMfaSettings(server, enabled)
                if (response.success && !enabled) {
                    userPreferenceDAO.updateMfa(
                        server.serverDomain,
                        server.profile.userId,
                        false
                    )
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.success("" to "")
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)
                val message = when (parsedError.code) {
                    1069 -> "Account is locked" to "Your account has been locked out due to too many attempts. Please try again later!"
                    else -> "" to parsedError.message
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", message, error = parsedError.cause)
            } catch (exception: Exception) {
                printlnCK("updateMfaSettings: $exception")
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", "" to exception.toString())
            }
        }

    override suspend fun sendMfaAuthChallenge(
        server: com.clearkeep.domain.model.Server,
        aHex: String
    ): UserOuterClass.MfaAuthChallengeResponse = withContext(Dispatchers.IO) {
        return@withContext userService.mfaAuthChallenge(server, aHex)
    }

    override suspend fun mfaValidatePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String,
        mHex: String
    ): com.clearkeep.common.utilities.network.Resource<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val validateResponse = userService.mfaValidatePassword(server, aHex, mHex)
            return@withContext if (validateResponse.success) com.clearkeep.common.utilities.network.Resource.success("" to "") else com.clearkeep.common.utilities.network.Resource.error(
                "",
                "" to validateResponse.toString()
            )
        } catch (exception: StatusRuntimeException) {
            printlnCK("mfaValidatePassword: $exception")

            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1001 -> "Error" to "The password is incorrect. Try again"
                1069 -> "Warning" to "Your account has been locked out due to too many attempts. Please try again later!"
                else -> "Error" to parsedError.message
            }
            return@withContext com.clearkeep.common.utilities.network.Resource.error("", message, error = parsedError.cause)
        } catch (exception: Exception) {
            printlnCK("mfaValidatePassword: $exception")
            return@withContext com.clearkeep.common.utilities.network.Resource.error("", "" to exception.toString())
        }
    }

    override suspend fun mfaValidateOtp(
        server: com.clearkeep.domain.model.Server,
        owner: com.clearkeep.domain.model.Owner,
        otp: String
    ): com.clearkeep.common.utilities.network.Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.mfaValidateOtp(server, otp)
                printlnCK("mfaValidateOtp success? ${response.success} error? ${response.error} code ${response.error}")
                return@withContext if (response.success) {
                    userPreferenceDAO.updateMfa(owner.domain, owner.clientId, true)
                    com.clearkeep.common.utilities.network.Resource.success(null)
                } else {
                    com.clearkeep.common.utilities.network.Resource.error(response.error, null)
                }
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1071 -> "Authentication failed. Please retry."
                    1068, 1072 -> "Verification code has expired. Please request a new code and retry."
                    else -> parsedError.message
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.error(message, null, error = parsedError.cause)
            } catch (exception: Exception) {
                printlnCK("mfaValidateOtp: $exception")
                return@withContext com.clearkeep.common.utilities.network.Resource.error(exception.toString(), null)
            }
        }

    override suspend fun mfaResendOtp(server: com.clearkeep.domain.model.Server): com.clearkeep.common.utilities.network.Resource<Pair<Int, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.mfaRequestResendOtp(server)
                printlnCK("mfaResendOtp success? ${response.success} error? ${response.error} code $response")
                return@withContext if (response.success) com.clearkeep.common.utilities.network.Resource.success(null) else com.clearkeep.common.utilities.network.Resource.error(
                    "",
                    0 to response.error
                )
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                    else -> parsedError.message
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.error(
                    "",
                    parsedError.code to message,
                    error = parsedError.cause
                )
            } catch (exception: Exception) {
                printlnCK("mfaResendOtp: $exception")
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", 0 to exception.toString())
            }
        }

    override suspend fun requestChangePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String
    ): UserOuterClass.RequestChangePasswordRes = withContext(Dispatchers.IO) {
        return@withContext userService.requestChangePassword(server, aHex)
    }

    override suspend fun changePassword(
        server: com.clearkeep.domain.model.Server,
        aHex: String,
        mHex: String,
        verificatorHex: String,
        newSaltHex: String,
        ivParam: String,
        identityKeyEncrypted: String?
    ): com.clearkeep.common.utilities.network.Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val changePasswordResponse = userService.changePassword(
                    server,
                    aHex,
                    mHex,
                    verificatorHex,
                    newSaltHex,
                    ivParam,
                    identityKeyEncrypted
                )
                return@withContext if (changePasswordResponse.error.isNullOrBlank()) com.clearkeep.common.utilities.network.Resource.success(
                    null
                ) else com.clearkeep.common.utilities.network.Resource.error(
                    "",
                    null
                )
            } catch (exception: StatusRuntimeException) {
                val parsedError = parseError(exception)
                val message = when (parsedError.code) {
                    1001, 1079 -> {
                        "The password is incorrect. Try again"
                    }
                    else -> parsedError.message
                }
                return@withContext com.clearkeep.common.utilities.network.Resource.error(message, null, error = parsedError.cause)
            } catch (exception: Exception) {
                printlnCK("changePassword: $exception")
                return@withContext com.clearkeep.common.utilities.network.Resource.error("", null)
            }
        }
}
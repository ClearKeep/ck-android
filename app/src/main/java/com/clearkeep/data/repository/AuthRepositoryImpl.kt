package com.clearkeep.data.repository

import auth.AuthOuterClass
import com.clearkeep.data.repository.*
import com.clearkeep.domain.model.LoginResponse
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.UserKey
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.model.SignalIdentityKey
import com.clearkeep.domain.repository.*
import com.clearkeep.srp.NativeLib
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.service.AuthService
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.fromHex
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPrivateKey
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userManager: AppStorage,
    private val serverRepository: ServerRepository, //TODO: Clean
    private val myStore: InMemorySignalProtocolStore,
    private val userPreferenceRepository: UserPreferenceRepository, //TODO: Clean
    private val environment: Environment,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val roomRepository: GroupRepository, //TODO: Clean
    private val userKeyRepository: UserKeyRepository, //TODO: Clean
    private val messageRepository: MessageRepository, //TODO: Clean
    private val authService: AuthService
) : AuthRepository {
    override suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<AuthOuterClass.RegisterSRPRes> = withContext(Dispatchers.IO) {
        val nativeLib = NativeLib()

        val salt = nativeLib.getSalt(email, password)
        val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val verificator = nativeLib.getVerificator()
        val verificatorHex =
            verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryCreateAccount()

        val decrypter = DecryptsPBKDF2(password)
        val key = KeyHelper.generateIdentityKeyPair()
        val preKeys = KeyHelper.generatePreKeys(1, 1)
        val preKey = preKeys[0]
        val signedPreKey = KeyHelper.generateSignedPreKey(key, (email + domain).hashCode())
        val transitionID = KeyHelper.generateRegistrationId(false)
        val iv = toHex(decrypter.getIv())
        val identityKeyPublic = key.publicKey.serialize()
        val preKeyId = preKey.id
        val identityKeyEncrypted = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
            toHex(it)
        }

        try {
            val response = authService.registerSrp(
                domain,
                email,
                verificatorHex,
                saltHex,
                displayName,
                iv,
                transitionID,
                identityKeyPublic,
                preKeyId,
                preKey.serialize(),
                signedPreKey.id,
                signedPreKey.serialize(),
                identityKeyEncrypted,
                signedPreKey.signature
            )

            if (response.error.isNullOrBlank()) {
                return@withContext Resource.success(response)
            } else {
                printlnCK("register failed: ${response.error}")
                return@withContext Resource.error(response.error, null)
            }
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1002 -> "This email address is already being used"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("register error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun login(
        userName: String,
        password: String,
        domain: String
    ): Resource<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val nativeLib = NativeLib()
                val a = nativeLib.getA(userName, password)
                val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val response = authService.loginChallenge(userName, aHex, domain)

                val salt = response.salt
                val b = response.publicChallengeB

                val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
                val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                nativeLib.freeMemoryAuthenticate()

                val authResponse = authService.loginAuthenticate(userName, aHex, mHex, domain)

                if (authResponse.error.isNullOrBlank()) {
                    printlnCK("login successfully")
                    val accessToken = authResponse.accessToken

                    val requireOtp = accessToken.isNullOrBlank()
                    if (requireOtp) {
                        return@withContext Resource.success(
                            LoginResponse(
                                authResponse.accessToken,
                                authResponse.preAccessToken,
                                authResponse.sub,
                                authResponse.hashKey,
                                0,
                                authResponse.error
                            )
                        )
                    } else {
                        val profileResponse = onLoginSuccess(domain, password, authResponse, "")
                        if (profileResponse.status == Status.ERROR) {
                            return@withContext Resource.error(profileResponse.message ?: "", null)
                        }
                        return@withContext Resource.success(
                            LoginResponse(
                                authResponse.accessToken,
                                authResponse.requireAction,
                                authResponse.sub,
                                authResponse.hashKey,
                                0,
                                authResponse.error
                            )
                        )
                    }
                } else {
                    printlnCK("login failed: ${authResponse.error}")
                    return@withContext Resource.error("", null)
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                printlnCK("login error: ${e.message}")
                val errorMessage = when (parsedError.code) {
                    1001, 1079 -> "Please check your details and try again"
                    1026 -> "Your account has not been activated. Please check the email for the activation link."
                    1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                    else -> {
                        parsedError.message
                    }
                }
                return@withContext Resource.error(
                    errorMessage,
                    LoginResponse("", "", "", "", parsedError.code, errorMessage)
                )
            } catch (e: Exception) {
                printlnCK("login error: $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }

    override suspend fun loginByGoogle(
        token: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByGoogle(token, domain)

            return@withContext Resource.success(response)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by google error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    override suspend fun loginByFacebook(
        token: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByFacebook(token, domain)

            return@withContext Resource.success(response)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by facebook error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    override suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<AuthOuterClass.SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByMicrosoft(accessToken, domain)

            return@withContext Resource.success(response)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            printlnCK("login by microsoft error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by microsoft error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun registerSocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val nativeLib = NativeLib()

            val salt = nativeLib.getSalt(userName, rawPin)
            val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            val verificator = nativeLib.getVerificator()
            val verificatorHex =
                verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            nativeLib.freeMemoryCreateAccount()

            val decrypter = DecryptsPBKDF2(rawPin)
            val key = KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, (userName + domain).hashCode())
            val transitionID = KeyHelper.generateRegistrationId(false)
            val decryptResult = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
                toHex(it)
            }

            val identityKeyPublic = key.publicKey.serialize()
            val preKeyId = preKey.id
            val signedPreKeyId = signedPreKey.id
            val response = authService.registerPincode(
                transitionID,
                identityKeyPublic,
                preKey.serialize(),
                preKeyId,
                signedPreKeyId,
                signedPreKey.serialize(),
                decryptResult,
                signedPreKey.signature,
                userName,
                saltHex,
                verificatorHex,
                toHex(decrypter.getIv()),
                domain
            )
            if (response.error.isEmpty()) {
                printlnCK("registerSocialPin success ${response.requireAction}")
                val profileResponse =
                    onLoginSuccess(domain, rawPin, response, isSocialAccount = true)
                printlnCK("registerSocialPin get profile response $profileResponse")
                return@withContext profileResponse
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("registerSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("registerSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun verifySocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthOuterClass.AuthRes> =
        withContext(Dispatchers.IO) {
            try {
                val nativeLib = NativeLib()
                val a = nativeLib.getA(userName, rawPin)
                val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val challengeRes = authService.loginSocialChallenge(userName, aHex, domain)

                val salt = challengeRes.salt
                val b = challengeRes.publicChallengeB

                val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
                val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                nativeLib.freeMemoryAuthenticate()

                val response = authService.verifyPinCode(userName, aHex, mHex, domain)
                if (response.error.isEmpty()) {
                    printlnCK("verifySocialPin success ${response.requireAction}")
                    return@withContext onLoginSuccess(
                        domain,
                        rawPin,
                        response,
                        isSocialAccount = true
                    )
                }
                return@withContext Resource.error(response.error, null)
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                val message = when (parsedError.code) {
                    else -> parsedError.message
                }
                printlnCK("verifySocialPin error statusRuntime $e $message")
                return@withContext Resource.error(message, null)
            } catch (e: Exception) {
                printlnCK("verifySocialPin error $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }

    override suspend fun resetSocialPin(
        domain: String,
        rawPin: String,
        userName: String,
        resetPincodeToken: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val nativeLib = NativeLib()

            val salt = nativeLib.getSalt(userName, rawPin)
            val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            val verificator = nativeLib.getVerificator()
            val verificatorHex =
                verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            nativeLib.freeMemoryCreateAccount()

            val decrypter = DecryptsPBKDF2(rawPin)
            val key = KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, (userName + domain).hashCode())
            val transitionID = KeyHelper.generateRegistrationId(false)
            val decryptResult = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
                toHex(it)
            }

            val response = authService.resetPinCode(
                transitionID,
                key.publicKey.serialize(),
                preKey.serialize(),
                preKey.id,
                signedPreKey.serialize(),
                signedPreKey.id,
                decryptResult,
                signedPreKey.signature,
                userName,
                resetPincodeToken,
                verificatorHex,
                saltHex,
                toHex(decrypter.getIv()),
                domain
            )
            if (response.error.isEmpty()) {
                printlnCK("resetSocialPin success ${response.requireAction}")
                return@withContext onLoginSuccess(
                    domain,
                    rawPin,
                    response,
                    isSocialAccount = true,
                    clearOldUserData = true
                )
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("resetSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("resetSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun resetPassword(
        preAccessToken: String,
        email: String,
        domain: String,
        rawNewPassword: String,
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val nativeLib = NativeLib()

            val salt = nativeLib.getSalt(email, rawNewPassword)
            val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            val verificator = nativeLib.getVerificator()
            val verificatorHex =
                verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

            nativeLib.freeMemoryCreateAccount()

            val decrypter = DecryptsPBKDF2(rawNewPassword)
            val key = KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, (email + domain).hashCode())
            val transitionID = KeyHelper.generateRegistrationId(false)
            val decryptResult = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
                toHex(it)
            }

            val response = authService.forgotPasswordUpdate(
                transitionID,
                key.publicKey.serialize(),
                preKey.serialize(),
                preKey.id,
                signedPreKey.id,
                signedPreKey.serialize(),
                decryptResult,
                signedPreKey.signature,
                preAccessToken,
                email,
                verificatorHex,
                saltHex,
                toHex(decrypter.getIv()),
                domain
            )
            if (response.error.isEmpty()) {
                return@withContext onLoginSuccess(
                    domain,
                    rawNewPassword,
                    response,
                    clearOldUserData = true
                )
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("resetSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("resetSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun recoverPassword(
        email: String,
        domain: String
    ): Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val response = authService.forgotPassword(email, domain)

            if (response.error?.isEmpty() == true) {
                return@withContext Resource.success(response)
            } else {
                return@withContext Resource.error(response.error, null)
            }
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = parsedError.message
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("recoverPassword error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun logoutFromAPI(server: Server): Resource<AuthOuterClass.BaseResponse> =
        withContext(Dispatchers.IO) {
            printlnCK("logoutFromAPI")
            try {
                val deviceId = userManager.getUniqueDeviceID()
                val response = authService.logout(deviceId, server)

                if (response.error?.isEmpty() == true) {
                    printlnCK("logoutFromAPI successed")
                    return@withContext Resource.success(response)
                } else {
                    printlnCK("logoutFromAPI failed: ${response.error}")
                    return@withContext Resource.error(response.error, null)
                }
            } catch (e: Exception) {
                printlnCK("logoutFromAPI error: $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }

    override suspend fun validateOtp(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.validateOtp(otp, otpHash, userId, domain)
            val accessToken = response.accessToken
            val profile = getProfile(domain, accessToken, hashKey)
                ?: return@withContext Resource.error("Can not get profile", null)
            serverRepository.insertServer(
                Server(
                    serverName = response.workspaceName,
                    serverDomain = domain,
                    ownerClientId = profile.userId,
                    serverAvatar = "",
                    loginTime = getCurrentDateTime().time,
                    accessKey = accessToken,
                    hashKey = hashKey,
                    refreshToken = response.refreshToken,
                    profile = profile,
                )
            )
            userPreferenceRepository.initDefaultUserPreference(
                domain,
                profile.userId,
                isSocialAccount = false
            ) //TODO: CLEAN ARCHITECTURE move to use case
            return@withContext Resource.success(response)
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
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

    override suspend fun mfaResendOtp(
        domain: String,
        otpHash: String,
        userId: String
    ): Resource<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val response = authService.resendOtp(otpHash, userId, domain)
            printlnCK("mfaResendOtp oldOtpHash $otpHash newOtpHash? ${response.preAccessToken} success? ${response.success}")
            return@withContext if (response.preAccessToken.isNotBlank()) Resource.success(0 to response.preAccessToken) else Resource.error(
                "",
                0 to ""
            )
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> parsedError.message
            }
            return@withContext Resource.error("", parsedError.code to message)
        } catch (exception: Exception) {
            printlnCK("mfaResendOtp: $exception")
            return@withContext Resource.error("", 0 to exception.toString())
        }
    }

    private suspend fun onLoginSuccess(
        domain: String,
        password: String,
        response: AuthOuterClass.AuthRes,
        hashKey: String = response.hashKey,
        isSocialAccount: Boolean = false,
        clearOldUserData: Boolean = false
    ): Resource<AuthOuterClass.AuthRes> {
        try {
            val accessToken = response.accessToken
            val salt = response.salt
            val publicKey = response.clientKeyPeer.identityKeyPublic
            val privateKeyEncrypt = response.clientKeyPeer.identityKeyEncrypted
            val iv = response.ivParameter
            val privateKeyDecrypt = DecryptsPBKDF2(password).decrypt(
                fromHex(privateKeyEncrypt),
                fromHex(salt),
                fromHex(iv)
            )
            val preKey = response.clientKeyPeer.preKey
            val preKeyID = response.clientKeyPeer.preKeyId
            val preKeyRecord = PreKeyRecord(preKey.toByteArray())
            val signedPreKeyId = response.clientKeyPeer.signedPreKeyId
            val signedPreKey = response.clientKeyPeer.signedPreKey
            val signedPreKeyRecord = SignedPreKeyRecord(signedPreKey.toByteArray())
            val registrationID = response.clientKeyPeer.registrationId
            val clientId = response.clientKeyPeer.clientId

            val eCPublicKey: ECPublicKey =
                Curve.decodePoint(publicKey.toByteArray(), 0)
            val eCPrivateKey: ECPrivateKey =
                Curve.decodePrivatePoint(privateKeyDecrypt)
            val identityKeyPair = IdentityKeyPair(IdentityKey(eCPublicKey), eCPrivateKey)
            val signalIdentityKey =
                SignalIdentityKey(
                    identityKeyPair,
                    registrationID,
                    domain,
                    clientId,
                    response.ivParameter,
                    salt
                )
            val profile = getProfile(domain, accessToken, hashKey)
                ?: return Resource.error("Can not get profile", null)
            printlnCK("onLoginSuccess userId ${profile.userId}")
            printlnCK("insert signalIdentityKeyDAO")
            signalIdentityKeyDAO.insert(signalIdentityKey)

            environment.setUpTempDomain(
                Server(
                    null,
                    "",
                    domain,
                    profile.userId,
                    "",
                    0L,
                    "",
                    "",
                    "",
                    false,
                    Profile(null, profile.userId, "", "", "", 0L, "")
                )
            )
            myStore.storePreKey(preKeyID, preKeyRecord)
            myStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)

            if (clearOldUserData) {
                val oldServer = serverRepository.getServer(domain, profile.userId) //TODO: CLEAN ARCHITECTURE move to use case
                oldServer?.id?.let {
                    roomRepository.deleteGroup(domain, profile.userId) //TODO: CLEAN ARCHITECTURE move to use case
                    messageRepository.deleteMessageByDomain(domain, profile.userId) //TODO: CLEAN ARCHITECTURE move to use case
                }
            }

            serverRepository.insertServer(
                Server(
                    serverName = response.workspaceName,
                    serverDomain = domain,
                    ownerClientId = profile.userId,
                    serverAvatar = "",
                    loginTime = getCurrentDateTime().time,
                    accessKey = accessToken,
                    hashKey = hashKey,
                    refreshToken = response.refreshToken,
                    profile = profile,
                )
            ) //TODO: CLEAN ARCHITECTURE move to use case
            userPreferenceRepository.initDefaultUserPreference(
                domain,
                profile.userId,
                isSocialAccount
            ) //TODO: CLEAN ARCHITECTURE move to use case
            userKeyRepository.insert(UserKey(domain, profile.userId, salt, iv)) //TODO: CLEAN ARCHITECTURE move to use case
            printlnCK("onLoginSuccess insert server success")

            return Resource.success(response)
        } catch (e: Exception) {
            printlnCK("onLoginSuccess exception $e")
            return Resource.error(e.toString(), null)
        }
    }

    override suspend fun getProfile(domain: String, accessToken: String, hashKey: String): Profile? =
        withContext(Dispatchers.IO) {
            try {
                val response = authService.getProfile(domain, accessToken, hashKey)
                printlnCK("getProfileWithGrpc: $response")
                return@withContext Profile(
                    userId = response.id,
                    userName = response.displayName,
                    email = response.email,
                    phoneNumber = response.phoneNumber,
                    avatar = response.avatar,
                    updatedAt = Calendar.getInstance().timeInMillis
                )
            } catch (e: Exception) {
                printlnCK("getProfileWithGrpc: $e")
                return@withContext null
            }
        }
}
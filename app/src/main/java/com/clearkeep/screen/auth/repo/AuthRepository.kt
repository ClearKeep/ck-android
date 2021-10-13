package com.clearkeep.screen.auth.repo

import auth.AuthOuterClass
import com.clearkeep.db.clear_keep.model.LoginResponse
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.clear_keep.model.UserKey
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.model.SignalIdentityKey
import com.clearkeep.dragonsrp.NativeLib
import com.clearkeep.dynamicapi.CallCredentials
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.fromHex
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.google.protobuf.ByteString
import io.grpc.Status.DEADLINE_EXCEEDED
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
import user.UserGrpc
import user.UserOuterClass
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    // network
    private val paramAPIProvider: ParamAPIProvider,

    private val userManager: AppStorage,
    private val serverRepository: ServerRepository,
    private val myStore: InMemorySignalProtocolStore,
    private val signalKeyRepository: SignalKeyRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val environment: Environment,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val roomRepository: GroupRepository,
    private val userKeyRepository: UserKeyRepository,
    private val messageRepository: MessageRepository
) {
    suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<AuthOuterClass.RegisterSRPRes> = withContext(Dispatchers.IO) {
//        val decrypter = DecryptsPBKDF2(password)
//        val key= KeyHelper.generateIdentityKeyPair()
//        val preKeys = KeyHelper.generatePreKeys(1, 1)
//        val preKey = preKeys[0]
//        val signedPreKey = KeyHelper.generateSignedPreKey(key, (email+domain).hashCode())
//        val transitionID=KeyHelper.generateRegistrationId(false)
//        val setClientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
//            .setDeviceId(111)
//            .setRegistrationId(transitionID)
//            .setIdentityKeyPublic(ByteString.copyFrom(key.publicKey.serialize()))
//            .setPreKey(ByteString.copyFrom(preKey.serialize()))
//            .setPreKeyId(preKey.id)
//            .setSignedPreKeyId(signedPreKey.id)
//            .setSignedPreKey(ByteString.copyFrom(signedPreKey.serialize()))
//            .setIdentityKeyEncrypted(decrypter.encrypt(key.privateKey.serialize())?.let {
//                toHex(it)
//            }
//            )
//            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
//            .build()

        val nativeLib = NativeLib()

        val salt = nativeLib.getSalt(email, password)
        val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val verificator = nativeLib.getVerificator()
        val verificatorHex = verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val request = AuthOuterClass.RegisterSRPReq.newBuilder()
            .setWorkspaceDomain(domain)
            .setEmail(email)
            .setPasswordVerifier(verificatorHex)
            .setSalt(saltHex)
            .setDisplayName(displayName)
            .setAuthType(0L)
            .setFirstName("abc")
            .setLastName("abc")
////                .setHashPassword(DecryptsPBKDF2.md5(password))
////                .setClientKeyPeer(setClientKeyPeer)
////            .setSalt(decrypter.getSaltEncryptValue())
////                .setIvParameter(toHex(decrypter.getIv()))
            .build()
        try {
            val response =
                paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).registerSrp(request)
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

    suspend fun login(userName: String, password: String, domain: String): Resource<LoginResponse> =
        withContext(Dispatchers.IO) {
            printlnCK("login: $userName, password = $password, domain = $domain")
            try {
                val nativeLib = NativeLib()
                val a = nativeLib.getA(userName, password)
                val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val request = AuthOuterClass.AuthChallengeReq.newBuilder()
                    .setEmail(userName)
                    .setClientPublic(aHex)
                    .build()
                val response =
                    paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
                        .loginChallenge(request)

                val salt = response.salt
                printlnCK("salt bytestring ${ByteString.copyFrom(salt.decodeHex())}")
                val b = response.publicChallengeB

                val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
                val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

                val authReq = AuthOuterClass.AuthenticateReq.newBuilder()
                    .setEmail(userName)
                    .setClientPublic(aHex)
                    .setClientSessionKeyProof(mHex)
                    .build()

                val authResponse = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginAuthenticate(authReq)

                if (true) {
                    printlnCK("login successfully")
//                    val accessToken = response.accessToken

//                val requireOtp = accessToken.isNullOrBlank()
//                    if (true) {
                        return@withContext Resource.success(
                            LoginResponse(
                                "",
                                "",
                                "",
                                "",
                                0,
                                ""
                            )
                        )
//                    } else {
//                        val profileResponse = onLoginSuccess(domain, password, response)
//                        if (profileResponse.status == Status.ERROR) {
//                            return@withContext Resource.error(profileResponse.message ?: "", null)
//                        }
//                        return@withContext Resource.success(
//                            LoginResponse(
//                                "",
//                                "",
//                                "",
//                                "",
//                                0,
//                                ""
//                            )
//                        )
//                    }
                } else {
//                    printlnCK("login failed: ${response.error}")
                    return@withContext Resource.error("", null)
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                printlnCK("login error: ${e.message}")
                val errorMessage = when (parsedError.code) {
                    1001 -> "Please check your details and try again"
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

    suspend fun loginByGoogle(token:String, domain: String):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        printlnCK("loginByGoogle: token = $token, domain = $domain")
        try {
            val request=AuthOuterClass
                .GoogleLoginReq
                .newBuilder()
                .setIdToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response=paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).loginGoogle(request)

            if (response.error.isEmpty()) {
                printlnCK("login by google successfully: $response")
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.error, null)
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

    suspend fun loginByFacebook(token:String, domain: String):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        try {
            val request=AuthOuterClass
                .FacebookLoginReq
                .newBuilder()
                .setAccessToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response= paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).loginFacebook(request)

            if (response.error.isEmpty()) {
                printlnCK("login by facebook successfully: $response")
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.error, null)
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

    suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val request = AuthOuterClass
                .OfficeLoginReq
                .newBuilder()
                .setAccessToken(accessToken)
                .setWorkspaceDomain(domain)
                .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).loginOffice(request)
            if (response.error.isEmpty()) {
                printlnCK("login by microsoft successfully require action ${response.requireAction} pre access token ${response.preAccessToken} hashKey ${response.hashKey}")
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.error, null)
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

    suspend fun registerSocialPin(
        domain: String,
        rawPin: String,
        userId: String,
        preAccessToken: String
    ) = withContext(Dispatchers.IO) {
        try {
            val decrypter = DecryptsPBKDF2(rawPin)
            val key= KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, (userId+domain).hashCode())
            val transitionID=KeyHelper.generateRegistrationId(false)
            val decryptResult = decrypter.encrypt(
                key.privateKey.serialize()
            )?.let {
                toHex(
                    it
                )
            }

            val clientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
                .setDeviceId(111)
                .setRegistrationId(transitionID)
                .setIdentityKeyPublic(ByteString.copyFrom(key.publicKey.serialize()))
                .setPreKey(ByteString.copyFrom(preKey.serialize()))
                .setPreKeyId(preKey.id)
                .setSignedPreKeyId(signedPreKey.id)
                .setSignedPreKey(
                    ByteString.copyFrom(signedPreKey.serialize())
                )
                .setIdentityKeyEncrypted(
                    decryptResult
                )
                .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
                .build()

            val request = AuthOuterClass
                .RegisterPinCodeReq
                .newBuilder()
                .setPreAccessToken(preAccessToken)
                .setUserId(userId)
                .setHashPincode(DecryptsPBKDF2.md5(rawPin))
                .setSalt(decrypter.getSaltEncryptValue())
                .setClientKeyPeer(clientKeyPeer)
                .setIvParameter(toHex(decrypter.getIv()))
                .build()

            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).registerPincode(request)
            if (response.error.isEmpty()) {
                printlnCK("registerSocialPin success ${response.requireAction}")
                val profileResponse = onLoginSuccess(domain, rawPin, response, isSocialAccount = true)
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

    suspend fun verifySocialPin(domain: String, rawPin: String, userId: String, preAccessToken: String) = withContext(Dispatchers.IO) {
        try {
            val request = AuthOuterClass
                .VerifyPinCodeReq
                .newBuilder()
                .setPreAccessToken(preAccessToken)
                .setUserId(userId)
                .setHashPincode(DecryptsPBKDF2.md5(rawPin))
                .build()

            printlnCK("verifySocialPin rawPin $rawPin preAccessToken $preAccessToken userId $userId hashPincode ${DecryptsPBKDF2.md5(rawPin)}")

            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
            ).verifyPincode(request)
            if (response.error.isEmpty()) {
                printlnCK("verifySocialPin success ${response.requireAction}")
                return@withContext onLoginSuccess(domain, rawPin, response, isSocialAccount = true)
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

    suspend fun resetSocialPin(
        domain: String,
        rawPin: String,
        userId: String,
        preAccessToken: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val decrypter = DecryptsPBKDF2(rawPin)
            val key= KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, (userId+domain).hashCode())
            val transitionID=KeyHelper.generateRegistrationId(false)
            val decryptResult = decrypter.encrypt(
                key.privateKey.serialize()
            )?.let {
                toHex(
                    it
                )
            }

            val clientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
                .setDeviceId(111)
                .setRegistrationId(transitionID)
                .setIdentityKeyPublic(ByteString.copyFrom(key.publicKey.serialize()))
                .setPreKey(ByteString.copyFrom(preKey.serialize()))
                .setPreKeyId(preKey.id)
                .setSignedPreKeyId(signedPreKey.id)
                .setSignedPreKey(
                    ByteString.copyFrom(signedPreKey.serialize())
                )
                .setIdentityKeyEncrypted(
                    decryptResult
                )
                .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
                .build()

            val request = AuthOuterClass
                .ResetPinCodeReq
                .newBuilder()
                .setPreAccessToken(preAccessToken)
                .setUserId(userId)
                .setHashPincode(DecryptsPBKDF2.md5(rawPin))
                .setSalt(decrypter.getSaltEncryptValue())
                .setIvParameter(toHex(decrypter.getIv()))
                .setClientKeyPeer(clientKeyPeer)
                .build()

            val response =
                paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).resetPincode(request)
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

    suspend fun recoverPassword(
        email: String,
        domain: String
    ): Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val request = AuthOuterClass.FogotPassWord.newBuilder()
                .setEmail(email)
                .build()
            val response =
                paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).withDeadlineAfter(
                    REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                ).fogotPassword(request)
            if (response?.error?.isEmpty() == true) {
                return@withContext Resource.success(response)
            } else {
                return@withContext Resource.error(response.error, null)
            }
        }
        catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = parsedError.message
            return@withContext Resource.error(message, null, parsedError.code)
        }
        catch (e: Exception) {
            printlnCK("recoverPassword error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun logoutFromAPI(server: Server) : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("logoutFromAPI")
        try {
            val request = AuthOuterClass.LogoutReq.newBuilder()
                    .setDeviceId(userManager.getUniqueDeviceID())
                    .setRefreshToken(server.refreshToken)
                    .build()

            val  authBlockingWithHeader = paramAPIProvider.provideAuthBlockingStub(ParamAPI(server.serverDomain))
                    .withDeadlineAfter(10 * 1000, TimeUnit.MILLISECONDS)
                    .withCallCredentials(CallCredentials(server.accessKey, server.hashKey))
            val response = authBlockingWithHeader
                    .logout(request)
            if (response?.error?.isEmpty() == true) {
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

    suspend fun validateOtp(domain: String, otp: String, otpHash: String, userId: String, hashKey: String) : Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO)  {
        try {
            val request = AuthOuterClass.MfaValidateOtpRequest.newBuilder()
                .setOtpCode(otp)
                .setOtpHash(otpHash)
                .setUserId(userId)
                .build()
            val stub = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            val response = stub.validateOtp(request)
            val accessToken = response.accessToken
            printlnCK("validateOtp error? ${response.error}")
            printlnCK("validateOtp access token ${response.accessToken} domain $domain hashkey $hashKey")
            val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
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
            userPreferenceRepository.initDefaultUserPreference(domain, profile.userId, isSocialAccount = false)
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

    suspend fun mfaResendOtp(domain: String, otpHash: String, userId: String) : Resource<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val request = AuthOuterClass.MfaResendOtpReq.newBuilder()
                .setOtpHash(otpHash)
                .setUserId(userId)
                .build()
            val stub = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain))
            val response = stub.resendOtp(request)
            printlnCK("mfaResendOtp oldOtpHash $otpHash newOtpHash? ${response.otpHash} success? ${response.success}")
            return@withContext if (response.otpHash.isNotBlank()) Resource.success(0 to response.otpHash) else Resource.error("", 0 to "")
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

    private suspend fun loginSrp(username: String, rawPassword: String) = withContext(Dispatchers.IO) {
//        val nativeLib = NativeLib()
//
//        val a = nativeLib.getA(username, rawPassword).toUpperCase(Locale.ROOT)
//        printlnCK("Test call get A $a")
//        val newSaltAndB = nativeLib.testVerifyGetSalt(username, verificator, salt, a).toUpperCase(Locale.ROOT)
//        printlnCK("Test call new salt and b $newSaltAndB")
//        val newSalt = newSaltAndB.split(",")[0]
//        val b = newSaltAndB.split(",")[1]
//        printlnCK("Test call verify get new salt $newSalt")
//        printlnCK("Test call verify get B $b")
//
//        val m1 = nativeLib.getM1(salt, b).toUpperCase(Locale.ROOT)
//        printlnCK("Test call get M1 $m1")
//        val m2 = nativeLib.testVerifyGetM2(m1)
//        val kFromServer = nativeLib.testVerifyGetK(m1)
//        println("Test call verify get M2 $m2")
//        println("Test call verify get K $kFromServer")
//        val kFromClient = nativeLib.getK(m2)
//        println("Test call client has K $kFromClient")
//        println("Test call correct? ${kFromClient == kFromServer}")
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
            printlnCK("onLoginSuccess first step")
            val privateKeyDecrypt = DecryptsPBKDF2(password).decrypt(
                fromHex(privateKeyEncrypt),
                fromHex(salt),
                fromHex(iv)
            )
            printlnCK("onLoginSuccess decrypt private key success:signedPreKeyID:  ${response.clientKeyPeer.signedPreKeyId}  privateKeyDecrypt: ${privateKeyDecrypt}")
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
                SignalIdentityKey(identityKeyPair, registrationID, domain, clientId,response.ivParameter,salt)
            val profile = getProfile(
                paramAPIProvider.provideUserBlockingStub(
                    ParamAPI(
                        domain,
                        accessToken,
                        hashKey
                    )
                )
            )
                ?: return Resource.error("Can not get profile", null)
            printlnCK("insert signalIdentityKeyDAO")
            signalIdentityKeyDAO.insert(signalIdentityKey)


            environment.setUpTempDomain(Server(null, "", domain, profile.userId, "", 0L, "", "", "", false, Profile(null, profile.userId, "", "", "", 0L, "")))
            myStore.storePreKey(preKeyID, preKeyRecord)
            myStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)
            printlnCK("onLoginSuccess privateKey ${toHex(signalKeyRepository.getSignedKey().keyPair.privateKey.serialize())}")
            printlnCK("onLoginSuccess store key success")
            printlnCK("onLoginSuccess get key success")

            if (clearOldUserData) {
                val oldServer = serverRepository.getServer(domain, profile.userId)
                    oldServer?.id?.let {
                        roomRepository.removeGroupByDomain(domain, profile.userId)
                        messageRepository.clearMessageByDomain(domain, profile.userId)
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
            )
            userPreferenceRepository.initDefaultUserPreference(domain, profile.userId, isSocialAccount)
            userKeyRepository.insert(UserKey(domain, profile.userId, salt, iv))
            printlnCK("onLoginSuccess insert server success")

            return Resource.success(response)
        } catch (e: Exception) {
            printlnCK("onLoginSuccess exception $e")
            return Resource.error(e.toString(), null)
        }
    }

    private suspend fun getProfile(userGrpc: UserGrpc.UserBlockingStub) : Profile?  = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.Empty.newBuilder().build()
            val response = userGrpc.getProfile(request)
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
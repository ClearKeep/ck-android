package com.clearkeep.screen.auth.repo

import auth.AuthOuterClass
import com.clearkeep.db.clear_keep.model.LoginResponse
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.model.SignalIdentityKey
import com.clearkeep.dynamicapi.CallCredentials
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.SignalKeyRepository
import com.clearkeep.screen.chat.repo.UserPreferenceRepository
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.fromHex
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.ecc.ECPrivateKey
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import signal.SignalKeyDistributionGrpc
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
    ) {
    suspend fun register(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<AuthOuterClass.RegisterRes> = withContext(Dispatchers.IO) {
        val decrypter = DecryptsPBKDF2(password)
        printlnCK("register: $displayName, password = $password, domain = $domain")
        val key= KeyHelper.generateIdentityKeyPair()
        val testprivate=key.privateKey.serialize()

        printlnCK("private key : ${key.privateKey.serialize()}")

        printlnCK(
            "private key 4 ${
                    decrypter.encrypt(
                        key.privateKey.serialize()
                    )
            }"
        )

        val preKeys = KeyHelper.generatePreKeys(1, 1)
        val preKey = preKeys[0]
        val signedPreKey = KeyHelper.generateSignedPreKey(key, 5)
        val transitionID=KeyHelper.generateRegistrationId(false)
        val setClientKeyPeer = AuthOuterClass.PeerRegisterClientKeyRequest.newBuilder()
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
                decrypter.encrypt(
                    key.privateKey.serialize()
                )?.let {
                    toHex(
                        it
                    )
                }
            )
            .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
            .build()

        /*val test2=identityKeyPair.privateKey.serialize().toString()
        val test = decrypter.encrypt(identityKeyPair.privateKey.serialize().toString())
        identityKeyPair.privateKey
        val test3= test?.let {
            decrypter.decrypt(it, fromHex(decrypter.getSaltEncryptValue())) }

        printlnCK("privateKey: $test2")
        printlnCK("privateKey: encrypt$encrypt")
        printlnCK("privateKey: decrypt $test3")
*/
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                .setDisplayName(displayName)
                .setHashPassword(DecryptsPBKDF2.md5(password))
                .setEmail(email)
                .setWorkspaceDomain(domain)
                .setClientKeyPeer(setClientKeyPeer)
                .setSalt(decrypter.getSaltEncryptValue())
                .setIvParameterSpec(toHex(decrypter.getIv()))
                .build()
            printlnCK("decrypter: ${decrypter.getIv()}")
            val response =
                paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).register(request)
            if (response.error.isNullOrBlank()) {
                return@withContext Resource.success(response)
            } else {
                printlnCK("register failed: ${response.error}")
                return@withContext Resource.error(response.error, null)
            }

        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = parsedError.message
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("register error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun login(userName: String, password: String, domain: String): Resource<LoginResponse> =
        withContext(Dispatchers.IO) {
            printlnCK("login: $userName, password = $password, domain = $domain")
            try {
                val request = AuthOuterClass.AuthReq.newBuilder()
                    .setEmail(userName)
                    .setHashPassword(DecryptsPBKDF2.md5(password))
                    .setWorkspaceDomain(domain)
                    .setAuthType(1)
                    .build()
                val response =
                    paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).login(request)
                if (response.error.isEmpty()) {
                    printlnCK("login successfully")
                    val accessToken = response.accessToken
                    val hashKey = response.hashKey

                    val salt = response.salt
                    val publicKey = response.clientKeyPeer.identityKeyPublic
                    val privateKeyEncrypt = response.clientKeyPeer.identityKeyEncrypted
                    printlnCK("decrypter: ${response.ivParameterSpec}")
                    printlnCK(
                        "decrypter 2: ${
                            fromHex(response.ivParameterSpec)
                        }"
                    )
                    printlnCK("privateKeyDecrypt: $privateKeyEncrypt")
                    printlnCK("privateKeyDecrypt: ${privateKeyEncrypt}")


                    val privateKeyDecrypt = DecryptsPBKDF2(password).decrypt(
                        fromHex(privateKeyEncrypt),
                        fromHex(salt),
                        fromHex(response.ivParameterSpec)
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
                        Curve.decodePrivatePoint(privateKeyDecrypt.toByteArray())
                    val identityKeyPair = IdentityKeyPair(IdentityKey(eCPublicKey), eCPrivateKey)
                    val signalIdentityKey =
                        SignalIdentityKey(identityKeyPair, registrationID, domain, clientId)
                    signalIdentityKeyDAO.insert(signalIdentityKey)
                    myStore.storePreKey(preKeyID, preKeyRecord)
                    myStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)

                    //
                    val owner=Owner(domain,response.clientKeyPeer.clientId)
                    val address = CKSignalProtocolAddress(Owner(domain,response.clientKeyPeer.clientId), 111)

                        val profile = getProfile(
                        paramAPIProvider.provideUserBlockingStub(
                            ParamAPI(
                                domain,
                                accessToken,
                                hashKey
                            )
                        )
                    )
                    val requireOtp = accessToken.isNullOrBlank()
                    if (requireOtp) {
                        return@withContext Resource.success(
                            LoginResponse(
                                response.accessToken,
                                response.preAccessToken,
                                response.sub,
                                response.hashKey,
                                0,
                                response.error
                            )
                        )
                    } else {
//                        onLoginSuccess(domain, password, response, isSocialAccount = false)
                        return@withContext Resource.success(
                            LoginResponse(
                                response.accessToken,
                                response.requireAction,
                                response.sub,
                                response.hashKey,
                                0,
                                response.error
                            )
                        )
                    }
                } else {
                    printlnCK("login failed: ${response.error}")
                    return@withContext Resource.error(response.error, null)
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                printlnCK("login error: ${e.message}")
                val errorMessage = when (parsedError.code) {
                    1001 -> "Please check your details and try again"
                    1026 -> "Your account has not been activated. Please check the email for the activation link."
                    1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                    else -> parsedError.message
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
            val response=paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginGoogle(request)

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
            val response= paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginFacebook(request)

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
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginOffice(request)
            if (response.error.isEmpty()) {
                printlnCK("login by microsoft successfully require action ${response.requireAction} pre access token ${response.preAccessToken}")
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

    suspend fun registerSocialPin(domain: String, rawPin: String, userId: String, preAccessToken: String) = withContext(Dispatchers.IO) {
        try {
            val decrypter = DecryptsPBKDF2(rawPin)
            val key= KeyHelper.generateIdentityKeyPair()

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(key, 5)
            val transitionID=KeyHelper.generateRegistrationId(false)
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
                    decrypter.encrypt(
                        key.privateKey.serialize()
                    )?.let {
                        toHex(
                            it
                        )
                    }
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
                .setIvParameterSpec(toHex(decrypter.getIv()))
                .build()

            printlnCK("registerSocialPin rawPin $rawPin preAccessToken $preAccessToken userId $userId hashPincode ${DecryptsPBKDF2.md5(rawPin)}")

            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).registerPincode(request)
            if (response.error.isEmpty()) {
                printlnCK("registerSocialPin success ${response.requireAction}")
                val profileResponse = onLoginSuccess(domain, rawPin, response, isSocialAccount = true)
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

            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).verifyPincode(request)
            if (response.error.isEmpty()) {
                printlnCK("verifySocialPin success ${response.requireAction}")
                val profileResponse = onLoginSuccess(domain, rawPin, response, isSocialAccount = true)
                return@withContext profileResponse
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

    suspend fun recoverPassword(email: String, domain: String) : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val request = AuthOuterClass.FogotPassWord.newBuilder()
                    .setEmail(email)
                    .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).fogotPassword(request)
            if (response?.error?.isEmpty() == true) {
                return@withContext Resource.success(response)
            } else {
                return@withContext Resource.error(response.error, null)
            }
        }
        catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = parsedError.message
            return@withContext Resource.error(message, null)
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
                    .setRefreshToken("")
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
            val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
                Owner(domain, profile.userId),
                paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
            )
            if (!isRegisterKeySuccess) {
                return@withContext Resource.error("Can not register key", null)
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

    private suspend fun onLoginSuccess(domain: String, password: String, response: AuthOuterClass.AuthRes, isSocialAccount: Boolean = false) : Resource<AuthOuterClass.AuthRes> {
        val accessToken = response.accessToken
        val hashKey = response.hashKey

        val salt = response.salt
        val publicKey = response.clientKeyPeer.identityKeyPublic
        val privateKeyEncrypt = response.clientKeyPeer.identityKeyEncrypted
        printlnCK("decrypter: ${response.ivParameterSpec}")
        printlnCK(
            "decrypter 2: ${
                fromHex(response.ivParameterSpec)
            }"
        )
        printlnCK("privateKeyDecrypt: $privateKeyEncrypt")
        printlnCK("privateKeyDecrypt: ${privateKeyEncrypt}")

        val privateKeyDecrypt = DecryptsPBKDF2(password).decrypt(
            fromHex(privateKeyEncrypt),
            fromHex(salt),
            fromHex(response.ivParameterSpec)
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
            Curve.decodePrivatePoint(privateKeyDecrypt.toByteArray())
        val identityKeyPair = IdentityKeyPair(IdentityKey(eCPublicKey), eCPrivateKey)
        val signalIdentityKey =
            SignalIdentityKey(identityKeyPair, registrationID, domain, clientId)
        signalIdentityKeyDAO.insert(signalIdentityKey)
        myStore.storePreKey(preKeyID, preKeyRecord)
        myStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)

        val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
            ?: return Resource.error("Can not get profile", null)
        val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
            Owner(domain, profile.userId),
            paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
        )
        if (!isRegisterKeySuccess) {
            return Resource.error("Can not register key", null)
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

        return Resource.success(response)
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

    private suspend fun peerRegisterClientKeyWithGrpc(owner: Owner, signalGrpc: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("peerRegisterClientKeyWithGrpc, clientId = ${owner.clientId}, domain = ${owner.domain}")
        return@withContext true
    }
}

        /*try {
            *//*val address = CKSignalProtocolAddress(owner, 111)

            environment.setUpDomain(
                Server(
                    0,
                    "",
                    owner.domain,
                    owner.clientId,
                    "",
                    0,
                    "",
                    "",
                    "",
                    false,
                    Profile(null, "", "", "", "", 0L, "")
                )
            )*//*

*//*
            val identityKeyPair = myStore.generateIdentityKeyPair(owner.clientId, owner.domain)
*//*

*//*            val preKey = signalKeyRepository.getPreKey()
            val signedPreKey = signalKeyRepository.getSignedKey()*//*

            *//*val request = Signal.PeerRegisterClientKeyRequest.newBuilder()
                .setClientId(address.owner.clientId)
                .setDeviceId(address.deviceId)
                .setRegistrationId(myStore.localRegistrationId)
                .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPair.publicKey.serialize()))
                .setPreKey(ByteString.copyFrom(preKey.serialize()))
                .setPreKeyId(preKey.id)
                .setSignedPreKeyId(signedPreKey.id)
                .setSignedPreKey(
                    ByteString.copyFrom(signedPreKey.serialize())
                )
                .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
                .build()

            val response = signalGrpc.peerRegisterClientKey(request)
            if (response?.error?.isEmpty() == true) {
                printlnCK("peerRegisterClientKeyWithGrpc, success")
                return@withContext true
            }
        } catch (e: Exception) {
            printlnCK("peerRegisterClientKeyWithGrpc: $e")
        }

        return@withContext false
    }
}*/
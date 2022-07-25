package com.clearkeep.domain.usecase.auth

import android.util.Log
import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.decodeHex
import com.clearkeep.common.utilities.getCurrentDateTime
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.model.response.LoginResponse
import com.clearkeep.domain.model.response.SrpCreateAccountResponse
import com.clearkeep.domain.repository.*
import com.clearkeep.srp.NativeLibWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val environment: Environment,
    private val authRepository: AuthRepository,
    private val signalKeyRepository: SignalKeyRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val userKeyRepository: UserKeyRepository,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val inMemorySignalProtocolStore: SignalProtocolStore,
) {
    suspend fun byEmail(
        userName: String,
        password: String,
        domain: String
    ): Resource<LoginResponse> = withContext(Dispatchers.IO) {
        val userName = userName.trim()

        val nativeLib = NativeLibWrapper()
        val a = nativeLib.getA(userName, password)
        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val response = authRepository.sendLoginChallenge(userName, aHex, domain)

        if (response.isError() || response.data == null) {
            return@withContext Resource.error(
                "",
                LoginResponse(
                    "",
                    "",
                    "",
                    "",
                    response.errorCode,
                    response.message ?: ""
                )
            )
        }

        val salt = response.data!!.salt
        val b = response.data!!.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryAuthenticate()

        val authResponse = authRepository.loginAuthenticate(userName, aHex, mHex, domain)

        if (authResponse.isError() || authResponse.data == null || authResponse.data!!.error.isNotBlank()) {
            return@withContext Resource.error(
                "",
                LoginResponse(
                    "",
                    "",
                    "",
                    "",
                    authResponse.errorCode,
                    authResponse.message ?: ""
                )
            )
        }

        authResponse.data!!.run {
            val requireOtp = accessToken.isNullOrBlank()
            if (requireOtp) {
                return@withContext Resource.success(
                    LoginResponse(
                        accessToken,
                        preAccessToken,
                        sub,
                        hashKey,
                        0,
                        error
                    )
                )
            } else {
                val profileResponse = onLoginSuccess(domain, password, this, "")
                if (profileResponse.status == Status.ERROR) {
                    return@withContext Resource.error(profileResponse.message ?: "", null)
                }
                return@withContext Resource.success(
                    LoginResponse(
                        accessToken,
                        requireAction,
                        sub,
                        hashKey,
                        0,
                        error
                    )
                )
            }
        }
    }

    suspend fun resetPassword(
        preAccessToken: String,
        email: String,
        domain: String,
        rawNewPassword: String
    ): Resource<AuthRes> {
        val (saltHex, verificatorHex) = createAccountSrp(email, rawNewPassword)

        val decrypter = DecryptsPBKDF2(rawNewPassword)
        val bobPreKey = Curve.generateKeyPair()
        val bobIdentityKey: IdentityKeyPair = generateIdentityKeyPair()
        val registrationId = KeyHelper.generateRegistrationId(false)
        val bobSignedPreKey: SignedPreKeyRecord = generateSignedPreKey(bobIdentityKey, (email + domain).hashCode())
        val bobBundle = PreKeyBundle(registrationId, 111, 1, bobPreKey.publicKey, (email + domain).hashCode(), bobSignedPreKey.keyPair.publicKey, bobSignedPreKey.signature, bobIdentityKey.publicKey)
        val preKey = bobBundle.preKey
        val signedPreKey = generateSignedPreKey(bobIdentityKey, (email + domain).hashCode())
        val identityKeyPublic = bobIdentityKey.publicKey.serialize()
        val preKeyId = bobBundle.preKeyId
        val identityKeyEncrypted = decrypter.encrypt(bobIdentityKey.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }

        val response = authRepository.resetPassword(
            registrationId,
            identityKeyPublic,
            preKey.serialize(),
            preKeyId,
            signedPreKeyId = signedPreKey.id,
            signedPreKey.serialize(),
            identityKeyEncrypted,
            signedPreKey.signature,
            preAccessToken = preAccessToken,
            email,
            saltHex,
            verificatorHex,
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )
        if (response.isSuccess() && response.data != null) {
            val profileResponse = onLoginSuccess(domain, rawNewPassword, response.data!!, "")
            if (profileResponse.status == Status.ERROR) {
                return Resource.error(profileResponse.message ?: "", null)
            }
        }
        return response
    }

    suspend fun registerSocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthRes> {
        val (saltHex, verificatorHex) = createAccountSrp(userName, rawPin)

        val decrypter = DecryptsPBKDF2(rawPin)
        // create registrationId
        val registrationId = KeyHelper.generateRegistrationId(false)
        //PreKeyRecord
        val bobPreKeyPair = Curve.generateKeyPair()
        val preKeyRecord = PreKeyRecord(1,bobPreKeyPair)
        //SignedPreKey
        val signedPreKeyId = (userName + domain).hashCode()
        val bobIdentityKey: IdentityKeyPair = generateIdentityKeyPair()
        val signedPreKey = generateSignedPreKey(bobIdentityKey, signedPreKeyId)
        val identityKeyPublic = bobIdentityKey.publicKey.serialize()
        //Encrypt private key
        val identityKeyEncrypted = decrypter.encrypt(bobIdentityKey.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }

        val response = authRepository.registerSocialPin(
            registrationId,
            identityKeyPublic,
            preKeyRecord.serialize(),
            preKeyRecord.id,
            signedPreKeyId = signedPreKey.id,
            signedPreKey.serialize(),
            identityKeyEncrypted,
            signedPreKey.signature,
            userName,
            saltHex,
            verificatorHex,
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )
        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(domain, rawPin, response.data!!, isSocialAccount = true)
        }
        return Resource.error("", null, error = response.error)
    }


    suspend fun verifySocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthRes> {
        val nativeLib = NativeLibWrapper()
        val a = nativeLib.getA(userName, rawPin)
        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val challengeRes = authRepository.sendLoginSocialChallenge(userName, aHex, domain)

        if (challengeRes.isError() || challengeRes.data == null) {
            return Resource.error(
                challengeRes.message ?: "",
                null,
                challengeRes.errorCode,
                challengeRes.error
            )
        }

        val salt = challengeRes.data!!.salt
        val b = challengeRes.data!!.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryAuthenticate()

        val response = authRepository.verifySocialPin(userName, aHex, mHex, domain)
        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(
                domain,
                rawPin,
                response.data!!,
                isSocialAccount = true
            )
        }
        return Resource.error("", null, error = response.error)
    }

    suspend fun resetSocialPin(
        domain: String,
        rawPin: String,
        userName: String,
        resetPincodeToken: String
    ): Resource<AuthRes> {
        val nativeLib = NativeLibWrapper()

        val salt = nativeLib.getSalt(userName, rawPin)
        val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        val verificator = nativeLib.getVerificator()
        val verificatorHex =
            verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        nativeLib.freeMemoryCreateAccount()
        val decrypter = DecryptsPBKDF2(rawPin)

        // create registrationId
        val registrationId = KeyHelper.generateRegistrationId(false)
        //PreKeyRecord
        val bobPreKeyPair = Curve.generateKeyPair()
        val preKeyRecord = PreKeyRecord(1,bobPreKeyPair)
        //SignedPreKey
        val signedPreKeyId = (userName + domain).hashCode()
        val bobIdentityKey: IdentityKeyPair = generateIdentityKeyPair()
        val signedPreKey = generateSignedPreKey(bobIdentityKey, signedPreKeyId)
        val identityKeyPublic = bobIdentityKey.publicKey.serialize()
        //Encrypt private key
        val identityKeyEncrypted = decrypter.encrypt(bobIdentityKey.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }

        val response = authRepository.resetSocialPin(
            transitionID = registrationId,
            publicKey = identityKeyPublic,
            preKey = preKeyRecord.serialize(),
            preKeyId = preKeyRecord.id,
            signedPreKey = signedPreKey.serialize(),
            signedPreKeyId = signedPreKey.id,
            identityKeyEncrypted,
            signedPreKey.signature,
            userName,
            resetPincodeToken,
            verificatorHex,
            saltHex,
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )

        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(
                domain,
                rawPin,
                response.data!!,
                isSocialAccount = true,
                clearOldUserData = true
            )
        }
        return response
    }

    private suspend fun onLoginSuccess(
        domain: String,
        password: String,
        response: AuthRes,
        hashKey: String = response.hashKey,
        isSocialAccount: Boolean = false,
        clearOldUserData: Boolean = false
    ): Resource<AuthRes> {
        try {
            val accessToken = response.accessToken
            val salt = response.salt
            val publicKey = response.clientKeyPeer.identityKeyPublic
            val privateKeyEncrypt = response.clientKeyPeer.identityKeyEncrypted
            val iv = response.ivParameter
            val privateKeyDecrypt = DecryptsPBKDF2(password).decrypt(
                DecryptsPBKDF2.fromHex(privateKeyEncrypt),
                DecryptsPBKDF2.fromHex(salt),
                DecryptsPBKDF2.fromHex(iv)
            )
            val preKey = response.clientKeyPeer.preKey
            val preKeyID = response.clientKeyPeer.preKeyId
            val preKeyRecord = PreKeyRecord(preKey)
            val signedPreKeyId = response.clientKeyPeer.signedPreKeyId
            val signedPreKey = response.clientKeyPeer.signedPreKey
            val signedPreKeyRecord = SignedPreKeyRecord(signedPreKey)
            val registrationID = response.clientKeyPeer.registrationId
            val clientId = response.clientKeyPeer.clientId

            val eCPublicKey: ECPublicKey =
                Curve.decodePoint(publicKey, 0)
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
            val profile = authRepository.getProfile(domain, accessToken, hashKey)
                ?: return Resource.error("Can not get profile", null)
            printlnCK("onLoginSuccess userId ${profile.userId}")
            printlnCK("insert signalIdentityKeyDAO")
            signalKeyRepository.saveIdentityKey(signalIdentityKey)

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
            withContext(Dispatchers.IO) {
                inMemorySignalProtocolStore.storePreKey(preKeyID, preKeyRecord)
                inMemorySignalProtocolStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)
            }

            if (clearOldUserData) {
                val oldServer = serverRepository.getServer(domain, profile.userId)
                oldServer?.id?.let {
                    groupRepository.deleteGroup(domain, profile.userId)
                    messageRepository.deleteMessageByDomain(domain, profile.userId)
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
            userPreferenceRepository.initDefaultUserPreference(
                domain,
                profile.userId,
                isSocialAccount
            )

            printlnCK("LoginUseCase insert UserKey domain $domain userId ${profile.userId} salt $salt iv $iv")

            userKeyRepository.insert(
                UserKey(
                    domain,
                    profile.userId,
                    salt,
                    iv
                )
            )

            return Resource.success(response)
        } catch (e: Exception) {
            printlnCK("onLoginSuccess exception $e")
            return Resource.error(e.toString(), null)
        }
    }

    private fun createAccountSrp(username: String, password: String): SrpCreateAccountResponse {
        val nativeLib = NativeLibWrapper()

        val salt = nativeLib.getSalt(username, password)
        val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val verificator = nativeLib.getVerificator()
        val verificatorHex =
            verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryCreateAccount()

        return SrpCreateAccountResponse(saltHex, verificatorHex)
    }

    private fun generateIdentityKeyPair(): IdentityKeyPair {
        val identityKeyPairKeys = Curve.generateKeyPair()
        return IdentityKeyPair(
            IdentityKey(identityKeyPairKeys.publicKey),
            identityKeyPairKeys.privateKey
        )
    }

    @Throws(InvalidKeyException::class)
    private fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, keyPair.publicKey.serialize())
        return SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature)
    }

}
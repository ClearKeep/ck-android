package com.clearkeep.domain.usecase.auth

import auth.AuthOuterClass
import com.clearkeep.data.local.signal.model.SignalIdentityKey
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.*
import com.clearkeep.domain.repository.*
import com.clearkeep.srp.NativeLib
import com.clearkeep.utilities.DecryptsPBKDF2
import com.clearkeep.utilities.decodeHex
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
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
    private val inMemorySignalProtocolStore: InMemorySignalProtocolStore,
) {
    suspend fun byEmail(
        userName: String,
        password: String,
        domain: String
    ): Resource<LoginResponse> {
        val userName = userName.trim()

        val nativeLib = NativeLib()
        val a = nativeLib.getA(userName, password)
        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val response = authRepository.sendLoginChallenge(userName, aHex, domain)

        if (response.isError() || response.data == null) {
            return Resource.error(
                "",
                LoginResponse("", "", "", "", response.errorCode, response.message ?: "")
            )
        }

        val salt = response.data.salt
        val b = response.data.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryAuthenticate()

        val authResponse = authRepository.loginAuthenticate(userName, aHex, mHex, domain)

        if (authResponse.isError() || authResponse.data == null || authResponse.data.error.isNotBlank()) {
            return Resource.error(
                "",
                LoginResponse("", "", "", "", response.errorCode, response.message ?: "")
            )
        }

        authResponse.data.run {
            val requireOtp = accessToken.isNullOrBlank()
            if (requireOtp) {
                return Resource.success(
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
                    return Resource.error(profileResponse.message ?: "", null)
                }
                return Resource.success(
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
    ): Resource<AuthOuterClass.AuthRes> {
        val (saltHex, verificatorHex) = createAccountSrp(email, rawNewPassword)

        val decrypter = DecryptsPBKDF2(rawNewPassword)
        val key = KeyHelper.generateIdentityKeyPair()

        val preKeys = KeyHelper.generatePreKeys(1, 1)
        val preKey = preKeys[0]
        val signedPreKey = KeyHelper.generateSignedPreKey(key, (email + domain).hashCode())
        val transitionID = KeyHelper.generateRegistrationId(false)
        val decryptResult = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }

        val response = authRepository.resetPassword(
            transitionID, key.publicKey.serialize(), preKey.serialize(), preKey.id, signedPreKey.id,
            signedPreKey.serialize(),
            decryptResult,
            signedPreKey.signature,
            preAccessToken,
            email,
            verificatorHex,
            saltHex,
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )
        if (response.isSuccess() && response.data != null) {
            val profileResponse = onLoginSuccess(domain, rawNewPassword, response.data, "")
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
    ): Resource<AuthOuterClass.AuthRes> {
        val (saltHex, verificatorHex) = createAccountSrp(userName, rawPin)

        val decrypter = DecryptsPBKDF2(rawPin)
        val key = KeyHelper.generateIdentityKeyPair()

        val preKeys = KeyHelper.generatePreKeys(1, 1)
        val preKey = preKeys[0]
        val signedPreKey = KeyHelper.generateSignedPreKey(key, (userName + domain).hashCode())
        val transitionID = KeyHelper.generateRegistrationId(false)
        val decryptResult = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }

        val identityKeyPublic = key.publicKey.serialize()
        val preKeyId = preKey.id
        val signedPreKeyId = signedPreKey.id
        val response = authRepository.registerSocialPin(
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
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )
        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(domain, rawPin, response.data, isSocialAccount = true)
        }
        return Resource.error("", null, error = response.error)
    }

    suspend fun verifySocialPin(
        domain: String,
        rawPin: String,
        userName: String
    ): Resource<AuthOuterClass.AuthRes> {
        val nativeLib = NativeLib()
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

        val salt = challengeRes.data.salt
        val b = challengeRes.data.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryAuthenticate()

        val response = authRepository.verifySocialPin(userName, aHex, mHex, domain)
        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(
                domain,
                rawPin,
                response.data,
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
    ): Resource<AuthOuterClass.AuthRes> {
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
            DecryptsPBKDF2.toHex(it)
        }

        val response = authRepository.resetSocialPin(
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
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            domain
        )
        if (response.isSuccess() && response.data != null) {
            return onLoginSuccess(
                domain,
                rawPin,
                response.data,
                isSocialAccount = true,
                clearOldUserData = true
            )
        }
        return response
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
                DecryptsPBKDF2.fromHex(privateKeyEncrypt),
                DecryptsPBKDF2.fromHex(salt),
                DecryptsPBKDF2.fromHex(iv)
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
            userKeyRepository.insert(UserKey(domain, profile.userId, salt, iv))

            return Resource.success(response)
        } catch (e: Exception) {
            printlnCK("onLoginSuccess exception $e")
            return Resource.error(e.toString(), null)
        }
    }

    private fun createAccountSrp(username: String, password: String): SrpCreateAccountResponse {
        val nativeLib = NativeLib()

        val salt = nativeLib.getSalt(username, password)
        val saltHex = salt.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val verificator = nativeLib.getVerificator()
        val verificatorHex =
            verificator.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryCreateAccount()

        return SrpCreateAccountResponse(saltHex, verificatorHex)
    }
}
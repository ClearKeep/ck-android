package com.clearkeep.domain.usecase.auth

import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.srp.NativeLibWrapper
import org.whispersystems.libsignal.util.KeyHelper
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        displayName: String,
        password: String,
        email: String,
        domain: String
    ): Resource<Any> {
        val nativeLib = NativeLibWrapper()

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
        val identityKeyPublic = key.publicKey.serialize()
        val preKeyId = preKey.id
        val identityKeyEncrypted = decrypter.encrypt(key.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }
        val iv = DecryptsPBKDF2.toHex(decrypter.getIv())

        return authRepository.register(
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
    }
}
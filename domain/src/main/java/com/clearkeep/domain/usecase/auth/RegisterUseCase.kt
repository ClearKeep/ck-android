package com.clearkeep.domain.usecase.auth

import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.srp.NativeLibWrapper
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
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

        // create registrationId
        val registrationId = KeyHelper.generateRegistrationId(false)
        //PreKeyRecord
        val bobPreKeyPair = Curve.generateKeyPair()
        val preKeyRecord = PreKeyRecord(1, bobPreKeyPair)
        //SignedPreKey
        val signedPreKeyId = (email + domain).hashCode()
        val bobIdentityKey: IdentityKeyPair = generateIdentityKeyPair()
        val signedPreKey = generateSignedPreKey(bobIdentityKey, signedPreKeyId)
        val identityPublicKey = bobIdentityKey.publicKey.serialize()
        //Encrypt private key
        val identityKeyEncrypted = decrypter.encrypt(bobIdentityKey.privateKey.serialize(), saltHex)?.let {
            DecryptsPBKDF2.toHex(it)
        }
        val iv = DecryptsPBKDF2.toHex(decrypter.getIv())
        return authRepository.register(
            domain = domain,
            email = email,
            verificatorHex = verificatorHex,
            saltHex = saltHex,
            displayName = displayName,
            iv = iv,
            transitionID = registrationId,
            identityKeyPublic = identityPublicKey,
            preKeyId = preKeyRecord.id,
            preKey = preKeyRecord.serialize(),
            signedPreKeyId = signedPreKey.id,
            signedPreKey = signedPreKey.serialize(),
            identityKeyEncrypted = identityKeyEncrypted,
            signedPreKeySignature = signedPreKey.signature
        )
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
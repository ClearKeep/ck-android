package com.clearkeep.domain.usecase.profile

import com.clearkeep.data.repository.SignalKeyRepositoryImpl
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.srp.NativeLib
import com.clearkeep.utilities.DecryptsPBKDF2
import com.clearkeep.utilities.decodeHex
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository, private val signalKeyRepository: SignalKeyRepository) {
    suspend operator fun invoke(owner: Owner, email: String, oldPassword: String, newPassword: String): Resource<String> {
        val server = serverRepository.getServerByOwner(owner) ?: return Resource.error("", null)

        val nativeLib = NativeLib()
        val a = nativeLib.getA(email, oldPassword)

        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val response = profileRepository.requestChangePassword(server, aHex)

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

        val oldIdentityKey = signalKeyRepository.getIdentityKey(
            server.profile.userId,
            server.serverDomain
        )!!.identityKeyPair.privateKey.serialize()

        val decryptResult = decrypter.encrypt(
            oldIdentityKey,
            newSaltHex,
        )?.let {
            DecryptsPBKDF2.toHex(
                it
            )
        }

        return profileRepository.changePassword(
            server,
            aHex,
            mHex,
            verificatorHex,
            newSaltHex,
            DecryptsPBKDF2.toHex(decrypter.getIv()),
            decryptResult
        )
    }
}
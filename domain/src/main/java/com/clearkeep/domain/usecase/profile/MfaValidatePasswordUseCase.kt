package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.srp.NativeLib
import com.clearkeep.utilities.decodeHex
import com.clearkeep.common.utilities.network.Resource
import javax.inject.Inject

class MfaValidatePasswordUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(owner: com.clearkeep.domain.model.Owner, password: String): Resource<Pair<String, String>> {
        val server =
            serverRepository.getServerByOwner(owner) ?: return Resource.error(
                "",
                "" to ""
            )

        val nativeLib = NativeLib()
        val a = nativeLib.getA(server.profile.email ?: "", password)
        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val response = profileRepository.sendMfaAuthChallenge(server, aHex)

        val salt = response.salt
        val b = response.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        nativeLib.freeMemoryAuthenticate()

        return profileRepository.mfaValidatePassword(server, aHex, mHex)
    }
}
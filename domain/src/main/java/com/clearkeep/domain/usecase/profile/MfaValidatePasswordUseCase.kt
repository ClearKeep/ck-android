package com.clearkeep.domain.usecase.profile

import com.clearkeep.common.utilities.decodeHex
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.srp.NativeLib
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.toHexString
import javax.inject.Inject

class MfaValidatePasswordUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(owner: Owner, password: String): Resource<Pair<String, String>> {
        val server =
            serverRepository.getServerByOwner(owner) ?: return Resource.error(
                "",
                "" to ""
            )

        val nativeLib = NativeLib()
        val a = nativeLib.getA(server.profile.email ?: "", password)
        val aHex = a.toHexString()

        val response = profileRepository.sendMfaAuthChallenge(server, aHex)

        val salt = response.salt
        val b = response.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.toHexString()

        nativeLib.freeMemoryAuthenticate()

        return profileRepository.mfaValidatePassword(server, aHex, mHex)
    }
}
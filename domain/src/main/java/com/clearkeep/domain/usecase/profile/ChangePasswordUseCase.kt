package com.clearkeep.domain.usecase.profile

import android.util.Log
import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.decodeHex
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.srp.NativeLibWrapper
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.SenderKeyStore
import com.clearkeep.domain.usecase.auth.AuthenticationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val serverRepository: ServerRepository,
    private val signalKeyRepository: SignalKeyRepository,
    private val senderKeyStore: SenderKeyStore


) {
    suspend operator fun invoke(owner: Owner, email: String, oldPassword: String, newPassword: String): Resource<String> {
        val server = serverRepository.getServerByOwner(owner) ?: return Resource.error("", null)

        val nativeLib = NativeLibWrapper()
        val a = nativeLib.getA(email, oldPassword)

        val aHex = a.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

        val response = profileRepository.requestChangePassword(server, aHex)

        val salt = response.salt
        val b = response.publicChallengeB

        val m = nativeLib.getM(salt.decodeHex(), b.decodeHex())
        val mHex = m.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        nativeLib.freeMemoryAuthenticate()

        val newPasswordNativeLib = NativeLibWrapper()

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

    suspend fun loadSenderKey(senderKeyName: CKSignalProtocolAddress, groupID: Long): Pair<Long, ByteArray>? {
        return withContext(Dispatchers.IO) {
            try {
                val senderKey = senderKeyStore.loadSenderKey(senderKeyName, AuthenticationHelper.getUUID(groupId = groupID.toString(), senderKeyName.owner.clientId))
                senderKey?.let {
                    return@withContext Pair(first = groupID, senderKey.serialize())
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

   suspend fun updateKey(server: Server,arrayList: ArrayList<Pair<Long, ByteArray>>):
           Resource<String> = withContext(Dispatchers.IO){

        val privateKey = getIdentityPrivateKey(server.ownerClientId, server.serverDomain)
        val userKey = signalKeyRepository.getUserKey(server.serverDomain, server.ownerClientId)
        val encryptor = DecryptsPBKDF2(DecryptsPBKDF2.toHex(privateKey.serialize()))
       val listSenderEncrypt= arrayListOf<Pair<Long,ByteArray>>()
       arrayList.forEach {
           val encryptedGroupSenderKey = encryptor.encrypt(it.second, userKey.salt, userKey.iv)
           if (encryptedGroupSenderKey != null) {
               listSenderEncrypt.add(Pair(it.first,encryptedGroupSenderKey))
           }
       }
        return@withContext signalKeyRepository.updateGroupSenderKey(server = server,listSenderEncrypt)
    }

    private suspend fun getIdentityPrivateKey(
        clientId: String,
        domain: String
    ): ECPrivateKey {
        val identityKey = signalKeyRepository.getIdentityKey(clientId, domain)
        return identityKey?.identityKeyPair?.privateKey!!
    }
}
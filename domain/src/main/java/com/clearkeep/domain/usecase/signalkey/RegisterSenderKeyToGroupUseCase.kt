package com.clearkeep.domain.usecase.signalkey

import android.util.Log
import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.SenderKeyStore
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.domain.usecase.auth.AuthenticationHelper.Companion.getUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import javax.inject.Inject

class RegisterSenderKeyToGroupUseCase @Inject constructor(
    private val signalKeyRepository: SignalKeyRepository,
    private val serverRepository: ServerRepository,
    private val senderKeyStore: SenderKeyStore
) {
    suspend operator fun invoke(groupID: Long, clientId: String, domain: String): Resource<Any> = withContext(
        Dispatchers.IO) {
        val senderAddress = CKSignalProtocolAddress(
            Owner(
                domain,
                clientId
            ), groupID,SENDER_DEVICE_ID
        )
        if (!signalKeyRepository.hasSenderKey(senderAddress)) {
            Log.d("antx: ", "RegisterSenderKeyToGroupUseCase invoke line = 33: " );
            val aliceSessionBuilder = GroupSessionBuilder(senderKeyStore)
            val sentAliceDistributionMessage = aliceSessionBuilder.create(senderAddress,getUUID(groupId = groupID.toString(),senderAddress.owner.clientId))
            val senderKey = senderKeyStore.loadSenderKey(senderAddress, getUUID(groupId = groupID.toString(),senderAddress.owner.clientId))
            val server = serverRepository.getServer(domain, clientId)
                ?: return@withContext Resource.error("can not find server", null)
            val bobPreKeyPair = Curve.generateKeyPair()

            val privateKey = getIdentityPrivateKey(clientId, domain)
            val userKey = signalKeyRepository.getUserKey(domain, clientId)
            val encryptor = DecryptsPBKDF2(toHex(privateKey.serialize()))
            val encryptedGroupSenderKey =
                encryptor.encrypt(senderKey.serialize(), userKey.salt, userKey.iv)
            return@withContext signalKeyRepository.registerSenderKeyToGroup(
                server,
                senderAddress.deviceId,
                groupID,
                sentAliceDistributionMessage,
                sentAliceDistributionMessage.serialize(),
                bobPreKeyPair,
                1,
                encryptedGroupSenderKey
            )
        }
        return@withContext Resource.success("")
    }

    private suspend fun getIdentityPrivateKey(
        clientId: String,
        domain: String
    ): ECPrivateKey {
        val identityKey = signalKeyRepository.getIdentityKey(clientId, domain)
        return identityKey?.identityKeyPair?.privateKey!!
    }
}
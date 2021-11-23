package com.clearkeep.domain.usecase.signalkey

import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.utilities.DecryptsPBKDF2
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.utilities.printlnCK
import jdk.internal.loader.Resource
import org.whispersystems.libsignal.ecc.ECPrivateKey
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.util.KeyHelper
import java.security.interfaces.ECPrivateKey
import javax.inject.Inject

class RegisterSenderKeyToGroupUseCase @Inject constructor(
    private val signalKeyRepository: SignalKeyRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(groupID: Long, clientId: String, domain: String): Resource<Any> {
        val privateKey = getIdentityPrivateKey(clientId, domain)
        printlnCK("RegisterSenderKeyToGroupUseCase PK ${toHex(privateKey?.serialize()!!)}")

        val senderAddress = CKSignalProtocolAddress(
            Owner(
                domain,
                clientId
            ), 111)
        val groupSender = SenderKeyName(groupID.toString(), senderAddress)
        if (!signalKeyRepository.hasSenderKey(groupSender)) {
            printlnCK("RegisterSenderKeyToGroupUseCase hasSenderKey")
            val senderKeyID = KeyHelper.generateSenderKeyId()
            val senderKey = KeyHelper.generateSenderKey()
            val key = KeyHelper.generateSenderSigningKey()
            val senderKeyRecord = signalKeyRepository.loadSenderKey(groupSender)
            senderKeyRecord.setSenderKeyState(
                senderKeyID,
                0,
                senderKey,
                key
            )
            signalKeyRepository.storeSenderKey(groupSender, senderKeyRecord)
            val state = senderKeyRecord.senderKeyState
            val sentAliceDistributionMessage = SenderKeyDistributionMessage(
                state.keyId,
                state.senderChainKey.iteration,
                state.senderChainKey.seed,
                state.signingKeyPublic
            )

            //Encrypt sender key
            val userKey = signalKeyRepository.getUserKey(domain, domain)
            val encryptor = DecryptsPBKDF2(toHex(privateKey.serialize()))
            val encryptedGroupPrivateKey =
                encryptor.encrypt(key.privateKey.serialize(), userKey.salt, userKey.iv)

            val server = serverRepository.getServer(domain, clientId)
            if (server == null) {
                printlnCK("fetchNewGroup: ")
                return Resource.error("can not find server", null)
            }

            return signalKeyRepository.registerSenderKeyToGroup(
                server,
                senderAddress.deviceId,
                groupID,
                sentAliceDistributionMessage,
                encryptedGroupPrivateKey,
                key,
                senderKeyID,
                senderKey
            )
        }
        printlnCK("RegisterSenderKeyToGroupUseCase already has sender key")
        return Resource.success("")
    }

    private suspend fun getIdentityPrivateKey(
        clientId: String,
        domain: String
    ): ECPrivateKey? {
        val identityKey = signalKeyRepository.getIdentityKey(clientId, domain)
        return identityKey?.identityKeyPair?.privateKey
    }
}
package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.ServerDAO
import com.clearkeep.data.local.clearkeep.dao.UserKeyDAO
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.dao.SignalKeyDAO
import com.clearkeep.data.local.signal.dao.SignalPreKeyDAO
import com.clearkeep.data.local.signal.model.SignalIdentityKey
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.domain.repository.UserKeyRepository
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.UserKey
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.network.Resource
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.util.KeyHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyRepositoryImpl @Inject constructor(
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val signalPreKeyDAO: SignalPreKeyDAO,
    private val userKeyDAO: UserKeyDAO,
    private val serverDAO: ServerDAO,
    private val signalKeyDAO: SignalKeyDAO,
    private val signalKeyDistributionService: SignalKeyDistributionService
) : SignalKeyRepository {
    override fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey? =
        signalIdentityKeyDAO.getIdentityKey(clientId, domain)

    override suspend fun deleteKey(owner: Owner, server: Server, chatGroups: List<ChatGroup>?) {
        val (domain, clientId) = owner

        signalIdentityKeyDAO
            .deleteSignalKeyByOwnerDomain(domain, clientId)
        val senderAddress = CKSignalProtocolAddress(
            Owner(server.serverDomain, server.ownerClientId),
            RECEIVER_DEVICE_ID
        )

        signalPreKeyDAO.deleteSignalSenderKey(server.serverDomain, server.ownerClientId)

        chatGroups?.forEach { group ->
            val groupSender2 = SenderKeyName(group.groupId.toString(), senderAddress)
            senderKeyStore.deleteSenderKey(groupSender2)
            group.clientList.forEach {
                val senderAddress = CKSignalProtocolAddress(
                    Owner(
                        it.domain,
                        it.userId
                    ), SENDER_DEVICE_ID
                )
                val groupSender = SenderKeyName(group.groupId.toString(), senderAddress)
                signalKeyDAO.deleteSignalSenderKey(groupSender.groupId, groupSender.sender.name)
            }
        }
    }

    override suspend fun registerSenderKeyToGroup(groupID: Long, clientId: String, domain: String): Resource<Any> =
        withContext(Dispatchers.IO) {
            //get private key
            val identityKey = signalIdentityKeyDAO.getIdentityKey(clientId, domain)
            val privateKey = identityKey?.identityKeyPair?.privateKey
            //end get private key
            val senderAddress = CKSignalProtocolAddress(Owner(domain, clientId), 111)
            val groupSender = SenderKeyName(groupID.toString(), senderAddress)
            if (!senderKeyStore.hasSenderKey(groupSender)) {
                val senderKeyID = KeyHelper.generateSenderKeyId()
                val senderKey = KeyHelper.generateSenderKey()
                val key = KeyHelper.generateSenderSigningKey()
                val senderKeyRecord = senderKeyStore.loadSenderKey(groupSender)
                senderKeyRecord.setSenderKeyState(
                    senderKeyID,
                    0,
                    senderKey,
                    key
                )
                senderKeyStore.storeSenderKey(groupSender, senderKeyRecord)
                val state = senderKeyRecord.senderKeyState
                val sentAliceDistributionMessage = SenderKeyDistributionMessage(
                    state.keyId,
                    state.senderChainKey.iteration,
                    state.senderChainKey.seed,
                    state.signingKeyPublic
                )

                //Encrypt sender key
                val userKey = userKeyDAO.getKey(domain, domain) ?: UserKey(domain, domain, "", "")
                val encryptor = DecryptsPBKDF2(toHex(privateKey!!.serialize()))
                val encryptedGroupPrivateKey =
                    encryptor.encrypt(key.privateKey.serialize(), userKey.salt, userKey.iv)

                try {
                    val server = serverDAO.getServer(domain, clientId)
                    if (server == null) {
                        printlnCK("fetchNewGroup: ")
                        return@withContext Resource.error("can not find server", null)
                    }
                    val response = signalKeyDistributionService.registerClientKey(
                        server,
                        senderAddress.deviceId,
                        groupID,
                        sentAliceDistributionMessage,
                        encryptedGroupPrivateKey,
                        key,
                        senderKeyID,
                        senderKey
                    )
                    if (response?.error.isNullOrEmpty()) {
                        printlnCK("registerSenderKeyToGroup: $groupID: success")
                        return@withContext Resource.success(null)
                    }
                } catch (e: StatusRuntimeException) {
                    printlnCK("registerSenderKeyToGroup: $e")

                    val parsedError = parseError(e)
                    return@withContext Resource.error(parsedError.message, null, parsedError.code, parsedError.cause)
                } catch (e: Exception) {
                    printlnCK("registerSenderKeyToGroup: $e")
                    return@withContext Resource.error("", null, error = e)
                }
            }
            return@withContext Resource.error("", null)
        }
}
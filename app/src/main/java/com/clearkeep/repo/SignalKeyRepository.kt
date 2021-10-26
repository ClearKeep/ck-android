package com.clearkeep.repo

import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.dao.SignalKeyDAO
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.util.KeyHelper
import signal.Signal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyRepository @Inject constructor(
    // network calls
    private val senderKeyStore: InMemorySenderKeyStore,
    private val paramAPIProvider: ParamAPIProvider,
    private val serverRepository: ServerRepository,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val signalPreKeyDAO: SignalPreKeyDAO,
    private val signalKeyDAO: SignalKeyDAO
) {
    fun getIdentityKey(clientId: String, domain: String) =
        signalIdentityKeyDAO.getIdentityKey(clientId, domain)

    suspend fun deleteKey(owner: Owner, server: Server, chatGroups: List<ChatGroup>?) {
        val (domain, clientId) = owner

        signalIdentityKeyDAO
            .deleteSignalKeyByOwnerDomain(domain, clientId)
        val senderAddress = CKSignalProtocolAddress(Owner(server.serverDomain, server.ownerClientId), 222)

        signalPreKeyDAO.deleteSignalSenderKey(server.serverDomain, server.ownerClientId)

        chatGroups?.forEach { group ->
            val groupSender2 = SenderKeyName(group.groupId.toString(), senderAddress)
            senderKeyStore.deleteSenderKey(groupSender2)
            group.clientList.forEach {
                val senderAddress = CKSignalProtocolAddress(
                    Owner(
                        it.domain,
                        it.userId
                    ), 111
                )
                val groupSender = SenderKeyName(group.groupId.toString(), senderAddress)
                signalKeyDAO.deleteSignalSenderKey(groupSender.groupId, groupSender.sender.name)
            }
        }
    }

    suspend fun  registerSenderKeyToGroup(groupID: Long, clientId: String, domain: String) : Boolean = withContext(Dispatchers.IO) {
        //get private key
        val identityKey=signalIdentityKeyDAO.getIdentityKey(clientId, domain)
        val privateKey = identityKey?.identityKeyPair?.privateKey
        printlnCK("privateKey ${toHex(privateKey?.serialize()!!)} $clientId:$domain  toString: ${privateKey?.serialize()}  " )
        //end get private key
        val senderAddress = CKSignalProtocolAddress(Owner(domain, clientId), 111)
        val groupSender  =  SenderKeyName(groupID.toString(), senderAddress)
        printlnCK("registerSenderKeyToGroup: $groupID  hasSenderKey: ${senderKeyStore.hasSenderKey(groupSender)}")
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
            val request = Signal.GroupRegisterClientKeyRequest.newBuilder()
                .setDeviceId(senderAddress.deviceId)
                .setGroupId(groupID)
                .setClientKeyDistribution(ByteString.copyFrom(sentAliceDistributionMessage.serialize()))
                .setPrivateKey(toHex(key.privateKey.serialize()))
                .setPublicKey(ByteString.copyFrom(key.publicKey.serialize()))
                .setSenderKeyId(senderKeyID.toLong())
                .setSenderKey(ByteString.copyFrom(senderKey))
                .build()
            try {
                val server = serverRepository.getServer(domain = domain, ownerId = clientId)
                if (server == null) {
                    printlnCK("fetchNewGroup: can not find server")
                    NullPointerException("groupRegisterClientKey: can not find server")
                    return@withContext false
                }
                val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
                val response = withContext(Dispatchers.IO) {
                    paramAPIProvider.provideSignalKeyDistributionBlockingStub(paramAPI)
                        .groupRegisterClientKey(request)
                }
                if (response?.error.isNullOrEmpty()) {
                    printlnCK("registerSenderKeyToGroup: $groupID: success")
                    return@withContext true
                }
            } catch (e: StatusRuntimeException) {
                printlnCK("registerSenderKeyToGroup: $e")

                val parsedError = parseError(e)
                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("registerSenderKeyToGroup token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
            } catch (e: Exception) {
                printlnCK("registerSenderKeyToGroup: $e")
            }
        }
        return@withContext false
    }
}
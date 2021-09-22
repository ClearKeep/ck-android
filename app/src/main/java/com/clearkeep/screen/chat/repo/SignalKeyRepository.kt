package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import signal.Signal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyRepository @Inject constructor(
    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    private val senderKeyStore: InMemorySenderKeyStore,
    private val myStore: InMemorySignalProtocolStore,
    private val preKeyDAO: SignalPreKeyDAO,
    private val serverRepository: ServerRepository
) {
    suspend fun getPreKey() : PreKeyRecord {
        var preKeyRecord = preKeyDAO.getFirstUnSignedPreKey()?.preKeyRecord ?: null
        return if (preKeyRecord == null) {
            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            printlnCK("generate PreKeys: ${preKey.id}")
            myStore.storePreKey(preKey.id, preKey)
            preKey
        } else {
            PreKeyRecord(preKeyRecord)
        }
    }

    suspend fun getSignedKey() : SignedPreKeyRecord {
        var signedKeyRecord = preKeyDAO.getFirstSignedPreKey()?.preKeyRecord ?: null
        return if (signedKeyRecord == null) {
            val identityKeyPair = myStore.identityKeyPair
            val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 5)
            printlnCK("generate Signed PreKey: ${signedPreKey.id}")
            myStore.storeSignedPreKey(signedPreKey.id, signedPreKey)
            signedPreKey
        } else {
            SignedPreKeyRecord(signedKeyRecord)
        }
    }

    suspend fun registerSenderKeyToGroup(groupID: Long, clientId: String, domain: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("registerSenderKeyToGroup: $groupID")
        val senderAddress = CKSignalProtocolAddress(Owner(domain, clientId), 111)
        val groupSender  =  SenderKeyName(groupID.toString(), senderAddress)
        val aliceSessionBuilder = GroupSessionBuilder(senderKeyStore)
        val sentAliceDistributionMessage = aliceSessionBuilder.create(groupSender)

        val request = Signal.GroupRegisterClientKeyRequest.newBuilder()
/*
            .setClientId(senderAddress.owner.clientId)
*/
            .setDeviceId(senderAddress.deviceId)
            .setGroupId(groupID)
            .setClientKeyDistribution(ByteString.copyFrom(sentAliceDistributionMessage.serialize()))
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                dynamicAPIProvider.provideSignalKeyDistributionBlockingStub().groupRegisterClientKey(request)
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
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
        } catch (e: Exception) {
            printlnCK("registerSenderKeyToGroup: $e")
        }

        return@withContext false
    }
}
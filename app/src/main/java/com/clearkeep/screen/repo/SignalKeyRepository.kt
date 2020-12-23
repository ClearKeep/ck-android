package com.clearkeep.screen.repo

import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.Storage
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.util.KeyHelper
import signal.Signal
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

private const val IS_PEER_SIGNAL_KEY_REGISTERED = "is_peer_signal_key_registered"

@Singleton
class SignalKeyRepository @Inject constructor(
        private val storage: Storage,
        private val client: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        private val myStore: InMemorySignalProtocolStore,
        private val senderKeyStore: InMemorySenderKeyStore,
) {
    fun isPeerKeyRegistered() = storage.getBoolean(IS_PEER_SIGNAL_KEY_REGISTERED)

    suspend fun peerRegisterClientKey(clientId: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("peerRegisterClientKey")
        try {
            val address = SignalProtocolAddress(clientId, 111)

            val identityKeyPair = myStore.identityKeyPair

            val preKeys = KeyHelper.generatePreKeys(1, 1)
            val preKey = preKeys[0]
            val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 5)

            val request = Signal.PeerRegisterClientKeyRequest.newBuilder()
                    .setClientId(address.name)
                    .setDeviceId(address.deviceId)
                    .setRegistrationId(myStore.localRegistrationId)
                    .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPair.publicKey.serialize()))
                    .setPreKey(ByteString.copyFrom(preKey.serialize()))
                    .setPreKeyId(preKey.id)
                    .setSignedPreKeyId(5)
                    .setSignedPreKey(
                            ByteString.copyFrom(signedPreKey.serialize())
                    )
                    .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
                    .build()

            val response = client.peerRegisterClientKey(request)
            if (response?.success != false) {
                myStore.storePreKey(preKey.id, preKey)
                myStore.storeSignedPreKey(signedPreKey.id, signedPreKey)
                storage.setBoolean(IS_PEER_SIGNAL_KEY_REGISTERED, true)
                printlnCK("peerRegisterClientKey, success")
                return@withContext true
            }
        } catch (e: Exception) {
            printlnCK("register: $e")
        }

        return@withContext false
    }

    suspend fun registerSenderKeyToGroup(groupID: Long, clientId: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("joinInGroup: $groupID")
        val senderAddress = SignalProtocolAddress(clientId, 111)
        val groupSender  =  SenderKeyName(groupID.toString(), senderAddress)
        val aliceSessionBuilder = GroupSessionBuilder(senderKeyStore)
        val sentAliceDistributionMessage = aliceSessionBuilder.create(groupSender)

        val request = Signal.GroupRegisterClientKeyRequest.newBuilder()
            .setClientId(senderAddress.name)
            .setDeviceId(senderAddress.deviceId)
            .setGroupId(groupID)
            .setClientKeyDistribution(ByteString.copyFrom(sentAliceDistributionMessage.serialize()))
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.groupRegisterClientKey(request)
            }
            if (response?.success != false) {
                printlnCK("joinInGroup: $groupID: success")
                return@withContext true
            }
        } catch (e: Exception) {
            printlnCK("joinInGroup: $e")
        }

        return@withContext false
    }

    @Throws(Exception::class)
    suspend fun isRegisteredGroupKey(groupID: Long, clientId: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("isRegisteredGroupKey: $groupID")
        try {
            val request = Signal.GroupGetClientKeyRequest.newBuilder()
                    .setGroupId(groupID)
                    .setClientId(clientId)
                    .build()
            val response = client.groupGetClientKey(request)
            val ret = response != null && response.clientKey != null
            printlnCK("isRegisteredGroupKey: $ret")
            return@withContext ret
        } catch (e : StatusRuntimeException) {
            printlnCK("isRegisteredGroupKey: $e")
            if (e.status.code == Status.NOT_FOUND.code) return@withContext false else throw e
        } catch (e : Exception) {
            printlnCK("isRegisteredGroupKey: $e")
            throw e
        }
    }
}
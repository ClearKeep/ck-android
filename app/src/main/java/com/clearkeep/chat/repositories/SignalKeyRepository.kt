package com.clearkeep.chat.repositories

import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.Storage
import com.google.protobuf.ByteString
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

        private val roomRepository: RoomRepository,
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
            if (null != response?.message && response.message == "success") {
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

    suspend fun joinInGroup(roomId: Int, groupID: String, clientId: String) : Boolean = withContext(Dispatchers.IO) {
        val senderAddress = SignalProtocolAddress(clientId, 111)
        val groupSender  =  SenderKeyName(groupID, senderAddress)
        val aliceSessionBuilder = GroupSessionBuilder(senderKeyStore)
        val sentAliceDistributionMessage = aliceSessionBuilder.create(groupSender)

        val request = Signal.GroupRegisterClientKeyRequest.newBuilder()
            .setClientId(senderAddress.name)
            .setDeviceId(senderAddress.deviceId)
            .setGroupId(groupSender.groupId)
            .setClientKeyDistribution(ByteString.copyFrom(sentAliceDistributionMessage.serialize()))
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.groupRegisterClientKey(request)
            }
            if (null != response?.message && response.message == "success") {
                roomRepository.remarkJoinInRoom(roomId)
                return@withContext true
            }
        } catch (e: Exception) {
            printlnCK("joinInGroup: $e")
        }

        return@withContext false
    }
}
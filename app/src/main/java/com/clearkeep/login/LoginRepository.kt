package com.clearkeep.login

import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.storage.Storage
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.util.KeyHelper
import signalc.SignalKeyDistributionGrpc
import signalc.Signalc
import signalc_group.GroupSenderKeyDistributionGrpc
import signalc_group.SignalcGroup
import javax.inject.Inject
import javax.inject.Singleton

private const val CLIENT_ID = "client_id"

private const val GROUP_CLIENT_ID = "group_client_id"

@Singleton
class LoginRepository @Inject constructor(
    private val storage: Storage,
    private val client: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
    private val myStore: InMemorySignalProtocolStore,

    private val groupClient: GroupSenderKeyDistributionGrpc.GroupSenderKeyDistributionBlockingStub,
    private val senderKeyStore: InMemorySenderKeyStore
) {
    fun isSingleUserRegistered() = storage.getString(CLIENT_ID).isNotEmpty()

    fun getClientId() = storage.getString(CLIENT_ID)

    suspend fun register(clientID: String) : Boolean {
        val address = SignalProtocolAddress(clientID, 111)

        val identityKeyPair = myStore.identityKeyPair

        val preKeys = KeyHelper.generatePreKeys(1, 1)
        val preKey = preKeys[0]
        val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 5)

        myStore.storePreKey(preKey.id, preKey)
        myStore.storeSignedPreKey(signedPreKey.id, signedPreKey)

        val request = Signalc.SignalRegisterKeysRequest.newBuilder()
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

        try {
            val response = withContext(Dispatchers.IO) {
                client.registerBundleKey(request)
            }
            if (null != response?.message && response.message == "success") {
                storage.setString(CLIENT_ID, clientID)
                return true
            }
        } catch (e: Exception) {
            println("register: $e")
        }

        return false
    }

    fun isGroupUserRegistered() = storage.getString(GROUP_CLIENT_ID).isNotEmpty()

    fun getGroupClientId() = storage.getString(GROUP_CLIENT_ID)

    suspend fun registerGroup(groupClientID: String) : Boolean {
        val senderAddress = SignalProtocolAddress(groupClientID, 111)
        val groupSender  =  SenderKeyName("test_group", senderAddress)
        val aliceSessionBuilder = GroupSessionBuilder(senderKeyStore)
        val sentAliceDistributionMessage = aliceSessionBuilder.create(groupSender)

        val request = SignalcGroup.GroupRegisterSenderKeyRequest.newBuilder()
            .setClientId(senderAddress.name)
            .setDeviceId(senderAddress.deviceId)
            .setGroupId(groupSender.groupId)
            .setSenderKeyDistribution(ByteString.copyFrom(sentAliceDistributionMessage.serialize()))
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                groupClient.registerSenderKeyGroup(request)
            }
            if (null != response?.message && response.message == "success") {
                storage.setString(GROUP_CLIENT_ID, groupClientID)
                return true
            }
        } catch (e: Exception) {
            println("register: $e")
        }

        return false
    }
}
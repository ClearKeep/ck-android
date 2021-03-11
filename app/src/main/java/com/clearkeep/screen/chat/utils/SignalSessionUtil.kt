package com.clearkeep.screen.chat.utils

import android.text.TextUtils
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.*
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.lang.Exception
import signal.Signal
import signal.SignalKeyDistributionGrpc
import java.nio.charset.StandardCharsets

@Throws(Exception::class)
suspend fun decryptPeerMessage(
        fromClientId: String, message: ByteString,
        signalProtocolStore: InMemorySignalProtocolStore,
) : String = withContext(Dispatchers.IO) {
    if (message.isEmpty) {
        return@withContext ""
    }

    val signalProtocolAddress = SignalProtocolAddress(fromClientId, 222)
    val preKeyMessage = PreKeySignalMessage(message.toByteArray())

    val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
    val message = sessionCipher.decrypt(preKeyMessage)
    return@withContext String(message, StandardCharsets.UTF_8)
}

@Throws(Exception::class)
suspend fun decryptGroupMessage(
        fromClientId: String, groupId: Long, message: ByteString,
        senderKeyStore: InMemorySenderKeyStore,
        clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
) : String = withContext(Dispatchers.IO) {
    if (message.isEmpty) {
        return@withContext ""
    }

    val senderAddress = SignalProtocolAddress(fromClientId, 222)
    val groupSender = SenderKeyName(groupId.toString(), senderAddress)
    val bobGroupCipher = GroupCipher(senderKeyStore, groupSender)

    printlnCK("decryptGroupMessage: groupId=$groupId, fromClientId=$fromClientId")
    initSessionUserInGroup(
            groupId, fromClientId, groupSender,
            clientBlocking, senderKeyStore, false)
    var plaintextFromAlice = try {
        bobGroupCipher.decrypt(message.toByteArray())
    } catch (ex: Exception) {
        printlnCK("decryptGroupMessage, $ex")
        val initSessionAgain = initSessionUserInGroup(
                groupId, fromClientId, groupSender,
                clientBlocking, senderKeyStore, true)
        if (!initSessionAgain) {
            throw Exception("can not init session in group $groupId")
        }
        bobGroupCipher.decrypt(message.toByteArray())
    }

    return@withContext String(plaintextFromAlice, StandardCharsets.UTF_8)
}

suspend fun initSessionUserPeer(
        receiver: String,
        signalProtocolAddress: SignalProtocolAddress,
        clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        signalProtocolStore: InMemorySignalProtocolStore,
) : Boolean = withContext(Dispatchers.IO) {
    printlnCK("initSessionUserPeer with $receiver")
    if (TextUtils.isEmpty(receiver)) {
        return@withContext false
    }
    try {
        val requestUser = Signal.PeerGetClientKeyRequest.newBuilder()
                .setClientId(receiver)
                .build()
        val remoteKeyBundle = clientBlocking.peerGetClientKey(requestUser)

        val preKey = PreKeyRecord(remoteKeyBundle.preKey.toByteArray())
        val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey.toByteArray())
        val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic.toByteArray(), 0)

        val retrievedPreKey = PreKeyBundle(
            remoteKeyBundle.registrationId,
            signalProtocolAddress.deviceId,
            preKey.id,
            preKey.keyPair.publicKey,
            remoteKeyBundle.signedPreKeyId,
            signedPreKey.keyPair.publicKey,
            signedPreKey.signature,
            identityKeyPublic
        )

        val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)

        // Build a session with a PreKey retrieved from the server.
        sessionBuilder.process(retrievedPreKey)
        printlnCK("initSessionUserPeer: success")
        return@withContext true
    } catch (e: Exception) {
        printlnCK("initSessionUserPeer: $e")
    }

    return@withContext false
}

fun initSessionUserInGroup(
        groupId: Long, fromClientId: String, groupSender: SenderKeyName,
        clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        senderKeyStore: InMemorySenderKeyStore, isForceProcess: Boolean,
): Boolean {
    val senderKeyRecord: SenderKeyRecord = senderKeyStore.loadSenderKey(groupSender)
    if (senderKeyRecord.isEmpty || isForceProcess) {
        printlnCK("initSessionUserInGroup, process new session: group id = $groupId")
        try {
            val request = Signal.GroupGetClientKeyRequest.newBuilder()
                    .setGroupId(groupId)
                    .setClientId(fromClientId)
                    .build()
            val senderKeyDistribution = clientBlocking.groupGetClientKey(request)
            val receivedAliceDistributionMessage = SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution.toByteArray())
            val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
            bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
        } catch (e: Exception) {
            printlnCK("initSessionUserInGroup: $e")
        }
    }
    return true
}
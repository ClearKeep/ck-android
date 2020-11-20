package com.clearkeep.screen.chat.utils

import android.text.TextUtils
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.lang.Exception
import signal.Signal
import signal.SignalKeyDistributionGrpc

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
                remoteKeyBundle.deviceId,
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
        printlnCK("initSessionWithReceiver: success")
        return@withContext true
    } catch (e: Exception) {
        printlnCK("initSessionWithReceiver: $e")
    }

    return@withContext false
}

fun initSessionUserInGroup(
        value: Signal.Publication, groupSender: SenderKeyName,
        clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        senderKeyStore: InMemorySenderKeyStore,
): Boolean {
    printlnCK("initSessionUserInGroup")
    val senderKeyRecord: SenderKeyRecord = senderKeyStore.loadSenderKey(groupSender)
    if (senderKeyRecord.isEmpty) {
        try {
            val request = Signal.GroupGetClientKeyRequest.newBuilder()
                    .setGroupId(value.groupId)
                    .setClientId(value.fromClientId)
                    .build()
            val senderKeyDistribution = clientBlocking.groupGetClientKey(request)
            val receivedAliceDistributionMessage = SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution.toByteArray())
            val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
            bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
        } catch (e: Exception) {
            printlnCK("initSession: $e")
        }
    }
    return true
}
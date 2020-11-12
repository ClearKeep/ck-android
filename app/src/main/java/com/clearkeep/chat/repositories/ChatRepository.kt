package com.clearkeep.chat.repositories

import android.text.TextUtils
import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.MessageDAO
import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Message
import com.clearkeep.login.LoginRepository
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import signal.Signal
import signal.SignalKeyDistributionGrpc
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
        private val client: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
        private val loginRepository: LoginRepository,
        private val messageDAO: MessageDAO,
        private val roomDAO: RoomDAO
) {
    init {
        subscribe()
    }

    fun getClientId() = loginRepository.getClientId()

    fun getMessagesFromRoom(roomId: Int) = messageDAO.getMessages(roomId = roomId)

    private fun subscribe() {
        val request = Signal.SubscribeAndListenRequest.newBuilder()
                .setClientId(loginRepository.getClientId())
                .build()

        client.subscribe(request, object : StreamObserver<Signal.BaseResponse> {
            override fun onNext(response: Signal.BaseResponse?) {
                printlnCK("subscribe onNext ${response?.message}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
                printlnCK("subscribe onCompleted")
                listen()
            }
        })
    }

    private fun listen() {
        val request = Signal.SubscribeAndListenRequest.newBuilder()
            .setClientId(loginRepository.getClientId())
            .build()
        client.listen(request, object : StreamObserver<Signal.Publication> {
            override fun onNext(value: Signal.Publication) {
                printlnCK("Receive a message from : ${value.fromClientId}, groupId = ${value.groupId}")
                if (TextUtils.isEmpty(value.groupId)) {
                    decryptMessageFromPeer(value)
                } else {
                    decryptMessageFromGroup(value)
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("listen message occurs error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    // Peer Chat
    private fun decryptMessageFromPeer(value: Signal.Publication) {
        try {
            val senderId = value.fromClientId
            val signalProtocolAddress = SignalProtocolAddress(senderId, 111)
            val preKeyMessage = PreKeySignalMessage(value.message.toByteArray())

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(senderId, signalProtocolAddress)
                if (!initSuccess) {
                    return
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message = sessionCipher.decrypt(preKeyMessage)
            val result = String(message, StandardCharsets.UTF_8)

            val roomId = roomDAO.getRoomId(senderId) ?: 0
            printlnCK("decryptMessageFromPeer: $result, roomId = $roomId")
            messageDAO.insert(Message(senderId, result, result, roomId, getCurrentDateTime().time))
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    fun sendMessageInPeer(roomId: Int, receiver: String, msg: String) : Boolean {
        try {
            val signalProtocolAddress = SignalProtocolAddress(receiver, 111)

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(receiver, signalProtocolAddress)
                if (!initSuccess) {
                    return false
                }
            }

            initSessionUserPeer(receiver, signalProtocolAddress)

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))
            val messageAfterEncrypted = String(message.serialize(), StandardCharsets.UTF_8)

            val request = Signal.PublishRequest.newBuilder()
                .setClientId(receiver)
                .setFromClientId(loginRepository.getClientId())
                .setMessage(ByteString.copyFrom(message.serialize()))
                .build()

            clientBlocking.publish(request)
            messageDAO.insert(Message(getClientId(), msg, messageAfterEncrypted, roomId, getCurrentDateTime().time))

            printlnCK("send message success: $msg, encrypted: $messageAfterEncrypted")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return false
        }

        return true
    }

    private fun initSessionUserPeer(receiver: String, signalProtocolAddress: SignalProtocolAddress) : Boolean {
        if (TextUtils.isEmpty(receiver)) {
            return false
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
            return true
        } catch (e: java.lang.Exception) {
            printlnCK("initSessionWithReceiver: $e")
        }

        return false
    }

    // Group
    private fun decryptMessageFromGroup(value: Signal.Publication) {
        try {
            val senderAddress = SignalProtocolAddress(value.fromClientId, 111)
            val groupSender = SenderKeyName(value.groupId, senderAddress)
            val bobGroupCipher = GroupCipher(senderKeyStore, groupSender)

            val initSession = initSessionUserInGroup(value, groupSender)
            if (!initSession) {
                return
            }

            val plaintextFromAlice = bobGroupCipher.decrypt(value.message.toByteArray())
            val decryptMessage = String(value.message.toByteArray(), StandardCharsets.UTF_8)
            val result = String(plaintextFromAlice, StandardCharsets.UTF_8)
            val roomId = roomDAO.getRoomId(value.groupId) ?: 0
            printlnCK("before decrypt: $decryptMessage")
            printlnCK("after decrypt: $result, roomId = $roomId")
            messageDAO.insert(Message(
                    value.fromClientId, result,
                    decryptMessage,
                    roomId, getCurrentDateTime().time)
            )
        } catch (e: Exception) {
            printlnCK("decrypt error : $e")
        }
    }

    fun sendMessageToGroup(roomId: Int, groupId: String, msg: String) : Boolean {
        try {
            val senderAddress = SignalProtocolAddress(loginRepository.getClientId(), 111)
            val groupSender  =  SenderKeyName(groupId, senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(msg.toByteArray(charset("UTF-8")))
            val messageAfterEncrypted = String(ciphertextFromAlice, StandardCharsets.UTF_8)

            val request = Signal.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.name)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            clientBlocking.publish(request)
            messageDAO.insert(Message(getClientId(), msg, messageAfterEncrypted, roomId, getCurrentDateTime().time))

            printlnCK("send message success: $msg, encrypted: $messageAfterEncrypted")
            return true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return false
    }

    private fun initSessionUserInGroup(value: Signal.Publication, groupSender: SenderKeyName): Boolean {
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
}
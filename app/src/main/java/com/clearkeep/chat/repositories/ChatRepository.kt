package com.clearkeep.chat.repositories

import android.text.TextUtils
import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.chat.utils.initSessionUserInGroup
import com.clearkeep.chat.utils.initSessionUserPeer
import com.clearkeep.db.MessageDAO
import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.Room
import com.clearkeep.login.LoginRepository
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
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
        private val roomRepository: RoomRepository,
        private val signalKeyRepository: SignalKeyRepository
) {
    init {
        subscribe()
    }

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun getClientId() = loginRepository.getClientId()

    fun getMessagesFromRoom(roomId: Int) = messageDAO.getMessages(roomId = roomId)

    fun getMessagesFromAFriend(remoteId: String) = messageDAO.getMessagesFromAFriend(remoteId = remoteId)

    suspend fun sendMessageInPeer(receiver: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
        try {
            if (!signalKeyRepository.isPeerKeyRegistered()) {
                signalKeyRepository.peerRegisterClientKey(getClientId())
            }

            val signalProtocolAddress = SignalProtocolAddress(receiver, 111)

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(receiver, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return@withContext false
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                    sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))

            val request = Signal.PublishRequest.newBuilder()
                    .setClientId(receiver)
                    .setFromClientId(loginRepository.getClientId())
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            clientBlocking.publish(request)
            insertNewMessage(getClientId(), getClientId(), isGroup = false, msg)

            printlnCK("send message success: $msg")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun sendMessageToGroup(groupId: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
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
            insertNewMessage(groupId, getClientId(), isGroup = true, msg)

            printlnCK("send message success: $msg, encrypted: $messageAfterEncrypted")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

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
                scope.launch {
                    if (TextUtils.isEmpty(value.groupId)) {
                        decryptMessageFromPeer(value)
                    } else {
                        decryptMessageFromGroup(value)
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen message error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    private suspend fun decryptMessageFromPeer(value: Signal.Publication) {
        try {
            val senderId = value.fromClientId
            val signalProtocolAddress = SignalProtocolAddress(senderId, 111)
            val preKeyMessage = PreKeySignalMessage(value.message.toByteArray())

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(senderId, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message = sessionCipher.decrypt(preKeyMessage)
            val result = String(message, StandardCharsets.UTF_8)
            printlnCK("decryptMessageFromPeer: $result")

            insertNewMessage(senderId, senderId, isGroup = false, result)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    // Group
    private suspend fun decryptMessageFromGroup(value: Signal.Publication) {
        try {
            val senderAddress = SignalProtocolAddress(value.fromClientId, 111)
            val groupSender = SenderKeyName(value.groupId, senderAddress)
            val bobGroupCipher = GroupCipher(senderKeyStore, groupSender)

            val initSession = initSessionUserInGroup(value, groupSender, clientBlocking, senderKeyStore)
            if (!initSession) {
                return
            }

            val plaintextFromAlice = bobGroupCipher.decrypt(value.message.toByteArray())
            val result = String(plaintextFromAlice, StandardCharsets.UTF_8)
            printlnCK("decryptMessageFromGroup: $result")

            insertNewMessage(value.groupId, value.fromClientId, isGroup = true, result)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromGroup error : $e")
        }
    }

    private suspend fun insertNewMessage(remoteId: String, senderId: String, isGroup: Boolean, message: String) {
        val room = roomRepository.getRoomOrCreateIfNot(getClientId(), remoteId, isGroup)
        val createTime = getCurrentDateTime().time
        messageDAO.insert(Message(senderId, getClientId(), message, room.id, createTime))

        // update last message in room
        val updateRoom = Room(
                id = room.id,
                roomName = room.roomName,
                remoteId = room.remoteId,
                isGroup = room.isGroup,
                isAccepted = room.isAccepted,

                lastPeople = senderId,
                lastMessage = message,
                lastUpdatedTime = createTime,
                isRead = false,
        )
        roomRepository.updateRoom(updateRoom)
    }
}
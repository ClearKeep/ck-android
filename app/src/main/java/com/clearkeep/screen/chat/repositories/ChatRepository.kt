package com.clearkeep.screen.chat.repositories

import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.screen.chat.utils.*
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
import signal.Signal
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
        private val client: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,

        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,

        private val messageRepository: MessageRepository,
        private val roomRepository: GroupRepository,
        private val userRepository: ProfileRepository
) {
    fun initSubscriber() {
        subscribe()
    }

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun getClientId() = userRepository.getClientId()

    suspend fun sendMessageInPeer(receiverId: String, groupId: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageInPeer: sender=$senderId, receiver= $receiverId")
        try {
            val signalProtocolAddress = SignalProtocolAddress(receiverId, 111)

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(receiverId, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return@withContext false
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                    sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))

            val request = Signal.PublishRequest.newBuilder()
                    .setClientId(receiverId)
                    .setFromClientId(senderId)
                    .setGroupId(groupId)
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            clientBlocking.publish(request)
            insertNewMessage(groupId, senderId, msg, receiverId)

            printlnCK("send message success: $msg")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun sendMessageToGroup(groupId: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId")
        try {
            val senderAddress = SignalProtocolAddress(senderId, 111)
            val groupSender  =  SenderKeyName(groupId, senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(msg.toByteArray(charset("UTF-8")))

            val request = Signal.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.name)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            clientBlocking.publish(request)
            insertNewMessage(groupId, senderId, msg, "")

            printlnCK("send message success: $msg")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

    private fun subscribe() {
        val ourClientId = getClientId()
        val request = Signal.SubscribeAndListenRequest.newBuilder()
                .setClientId(ourClientId)
                .build()

        client.subscribe(request, object : StreamObserver<Signal.BaseResponse> {
            override fun onNext(response: Signal.BaseResponse?) {
                printlnCK("subscribe $ourClientId onNext ${response?.message}")
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
            .setClientId(getClientId())
            .build()
        client.listen(request, object : StreamObserver<Signal.Publication> {
            override fun onNext(value: Signal.Publication) {
                printlnCK("Receive a message from : ${value.fromClientId}" +
                        ", groupId = ${value.groupId} groupType = ${value.groupType}")
                scope.launch {
                    // TODO
                    if (!isGroup(value.groupType)) {
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
            val result = decryptPeerMessage(value.fromClientId, value.message, signalProtocolStore)
            insertNewMessage(value.groupId, value.fromClientId, getClientId(), result)

            printlnCK("decryptMessageFromPeer: $result")
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    private suspend fun decryptMessageFromGroup(value: Signal.Publication) {
        try {
            val result = decryptGroupMessage(value.fromClientId, value.groupId, value.message, senderKeyStore, clientBlocking)
            insertNewMessage(value.groupId, value.fromClientId, getClientId(), result)

            printlnCK("decryptMessageFromGroup: $result")
        } catch (e: Exception) {
            printlnCK("decryptMessageFromGroup error : $e")
        }
    }

    private suspend fun insertNewMessage(groupId: String, senderId: String,
                                         receiverId: String, message: String) {
        var room: ChatGroup? = roomRepository.getGroupByID(groupId)
        if (room == null) {
            room = roomRepository.getGroupFromAPI(groupId)
            if (room != null) {
                roomRepository.insertGroup(room)
            }
        }

        if (room == null) {
            return
        }

        val createTime = getCurrentDateTime().time
        messageRepository.insert(Message(senderId, message, room.id, createTime, createTime, receiverId))

        // update last message in room
        val updateRoom = ChatGroup(
                id = room.id,
                groupName = room.groupName,
                groupAvatar = room.groupAvatar,
                groupType = room.groupType,
                createBy = room.createBy,
                createdAt = room.createdAt,
                updateBy = room.updateBy,
                updateAt = room.updateAt,
                clientList = room.clientList,

                // update
                isJoined = true,

                lastClient = senderId,
                lastMessage = message,
                lastUpdatedTime = createTime
        )
        roomRepository.updateRoom(updateRoom)
    }
}
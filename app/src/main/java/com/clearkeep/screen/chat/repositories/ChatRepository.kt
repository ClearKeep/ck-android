package com.clearkeep.screen.chat.repositories

import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import message.MessageGrpc
import message.MessageOuterClass
import notification.NotifyGrpc
import notification.NotifyOuterClass
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        private val notifyStub: NotifyGrpc.NotifyStub,
        private val messageGrpc: MessageGrpc.MessageStub,
        private val messageBlockingGrpc: MessageGrpc.MessageBlockingStub,

        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,

        private val messageRepository: MessageRepository,
        private val roomRepository: GroupRepository,
        private val userManager: UserManager
) {
    fun initSubscriber() {
        subscribeMessageChannel()
        subscribeNotificationChannel()
    }

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun getClientId() = userManager.getClientId()

    suspend fun sendMessageInPeer(receiverId: String, groupId: String, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
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
                    sessionCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setClientId(receiverId)
                    .setFromClientId(senderId)
                    .setGroupId(groupId)
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            val response = messageBlockingGrpc.publish(request)
            insertNewMessage(response, plainMessage)

            printlnCK("send message success: $plainMessage")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun sendMessageToGroup(groupId: String, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId")
        try {
            val senderAddress = SignalProtocolAddress(senderId, 111)
            val groupSender  =  SenderKeyName(groupId, senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.name)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            val response = messageBlockingGrpc.publish(request)
            insertNewMessage(response, plainMessage)

            printlnCK("send message success: $plainMessage")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

    private fun subscribeMessageChannel() {
        val ourClientId = getClientId()
        printlnCK("subscribeMessageChannel: $ourClientId")
        val request = MessageOuterClass.SubscribeAndListenRequest.newBuilder()
                .setClientId(ourClientId)
                .build()

        messageGrpc.subscribe(request, object : StreamObserver<MessageOuterClass.BaseResponse> {
            override fun onNext(response: MessageOuterClass.BaseResponse?) {
                printlnCK("subscribe message  $ourClientId onNext ${response?.success}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
                printlnCK("subscribe message onCompleted")
                listenMessageChannel()
            }
        })
    }

    private fun listenMessageChannel() {
        val request = MessageOuterClass.SubscribeAndListenRequest.newBuilder()
                .setClientId(getClientId())
                .build()
        messageGrpc.listen(request, object : StreamObserver<MessageOuterClass.MessageObjectResponse> {
            override fun onNext(value: MessageOuterClass.MessageObjectResponse) {
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

    private fun subscribeNotificationChannel() {
        printlnCK("subscribeNotificationChannel")
        val ourClientId = getClientId()
        val request = NotifyOuterClass.SubscribeAndListenRequest.newBuilder()
                .setClientId(ourClientId)
                .build()

        notifyStub.subscribe(request, object : StreamObserver<NotifyOuterClass.BaseResponse> {
            override fun onNext(response: NotifyOuterClass.BaseResponse?) {
                printlnCK("subscribe notification $ourClientId onNext ${response?.success}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
                printlnCK("subscribe notification onCompleted")
                listenNotificationChannel()
            }
        })
    }

    private fun listenNotificationChannel() {
        val request = NotifyOuterClass.SubscribeAndListenRequest.newBuilder()
                .setClientId(getClientId())
                .build()
        notifyStub.listen(request, object : StreamObserver<NotifyOuterClass.NotifyObjectResponse> {
            override fun onNext(value: NotifyOuterClass.NotifyObjectResponse) {
                value.createdAt
                printlnCK("Receive a notification from : ${value.refClientId}" +
                        ", groupId = ${value.refGroupId} groupType = ${value.notifyType}")
                scope.launch {
                    if(value.notifyType == "new-group") {
                        roomRepository.fetchRoomsFromAPI()
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen notification error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    private suspend fun decryptMessageFromPeer(value: MessageOuterClass.MessageObjectResponse) {
        try {
            val plainMessage = decryptPeerMessage(value.fromClientId, value.message, signalProtocolStore)
            insertNewMessage(value, plainMessage)

            printlnCK("decryptMessageFromPeer: $plainMessage")
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    private suspend fun decryptMessageFromGroup(value: MessageOuterClass.MessageObjectResponse) {
        try {
            val plainMessage = decryptGroupMessage(value.fromClientId, value.groupId, value.message, senderKeyStore, clientBlocking)
            insertNewMessage(value, plainMessage)

            printlnCK("decryptMessageFromGroup: $plainMessage")
        } catch (e: Exception) {
            printlnCK("decryptMessageFromGroup error : $e")
        }
    }

    private suspend fun insertNewMessage(value: MessageOuterClass.MessageObjectResponse, messageString: String) {
        val groupId = value.groupId
        var room: ChatGroup? = roomRepository.getGroupByID(groupId)
        if (room == null) {
            room = roomRepository.getGroupFromAPI(groupId)
            if (room != null) {
                roomRepository.insertGroup(room)
            }
        }

        if (room == null) {
            printlnCK("insertNewMessage error: can not a room with id $groupId")
            return
        }

        val messageRecord = convertMessageResponse(value, messageString)
        messageRepository.insert(messageRecord)

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

            lastMessage = messageRecord,
            lastMessageAt = messageRecord.createdTime
        )
        roomRepository.updateRoom(updateRoom)
    }

    private fun convertMessageResponse(value: MessageOuterClass.MessageObjectResponse, decryptedMessage: String): Message {
        return Message(
            value.id,
            value.groupId,
            value.groupType,
            value.fromClientId,
            value.clientId,
            decryptedMessage,
            value.createdAt,
            value.updatedAt,
        )
    }
}
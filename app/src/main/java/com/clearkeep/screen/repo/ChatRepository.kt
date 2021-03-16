package com.clearkeep.screen.repo

import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.ClientCallStreamObserver
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
import java.lang.Runnable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
        // network calls
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        private val notifyStubBlocking: NotifyGrpc.NotifyBlockingStub,
        private val messageBlockingGrpc: MessageGrpc.MessageBlockingStub,

        // data
        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
        private val userManager: UserManager,
        private val messageDAO: MessageDAO,

        private val groupRepository: GroupRepository,
) {
    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var roomId: Long = -1

    private var isNeedSubscribeMessageAgain = false

    private var isNeedSubscribeNotificationAgain = false

    private var messageHandler: MessageHandler? = null
    private var notificationHandler: NotificationHandler? = null

    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getJoiningRoomId() : Long {
        return roomId
    }

    fun getClientId() = userManager.getClientId()

    fun isNeedSubscribeAgain() : Boolean {
        return isNeedSubscribeMessageAgain || isNeedSubscribeNotificationAgain
    }

    suspend fun reInitSubscribe() {
        isNeedSubscribeMessageAgain = false
        isNeedSubscribeNotificationAgain = false

        unsubscribe()
        notificationHandler?.cancel("subscribe again", null)
        messageHandler?.cancel("subscribe again", null)
        delay(2 * 1000)
        subscribe()
    }

    suspend fun subscribe() {
        val messageSubscribeSuccess = subscribeMessageChannel()
        if (!messageSubscribeSuccess) {
            isNeedSubscribeMessageAgain = true
        }
        val notificationSuccess = subscribeNotificationChannel()
        if (!notificationSuccess) {
            isNeedSubscribeNotificationAgain = true
        }
    }

    private suspend fun unsubscribe() {
        unsubscribeMessageChannel()
        unsubscribeNotificationChannel()
    }

    private suspend fun subscribeMessageChannel() : Boolean = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.SubscribeRequest.newBuilder()
            .setClientId(getClientId())
            .build()

        try {
            val res = messageBlockingGrpc.subscribe(request)
            if (res.success) {
                printlnCK("subscribeMessageChannel, success")
                listenMessageChannel()
            } else {
                printlnCK("subscribeMessageChannel, ${res.errors}")
            }
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("subscribeMessageChannel, $e")
            return@withContext false
        }
    }

    private suspend fun unsubscribeMessageChannel() : Boolean = withContext(Dispatchers.IO) {
        try {
            val request = MessageOuterClass.UnSubscribeRequest.newBuilder()
                .setClientId(getClientId())
                .build()

            val res = messageBlockingGrpc.unSubscribe(request)
            printlnCK("unsubscribeMessageChannel, ${res.success}")
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("unsubscribeMessageChannel, $e")
            return@withContext false
        }
    }

    private suspend fun subscribeNotificationChannel() : Boolean = withContext(Dispatchers.IO) {
        val request = NotifyOuterClass.SubscribeRequest.newBuilder()
            .setClientId(getClientId())
            .build()
        try {
            val res = notifyStubBlocking.subscribe(request)
            if (res.success) {
                printlnCK("subscribeNotificationChannel, success")
                listenNotificationChannel()
            } else {
                printlnCK("subscribeNotificationChannel, ${res.errors}")
            }
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("subscribeNotificationChannel, $e")
            return@withContext false
        }
    }

    private suspend fun unsubscribeNotificationChannel() : Boolean = withContext(Dispatchers.IO) {
        try {
            val request = NotifyOuterClass.UnSubscribeRequest.newBuilder()
                .setClientId(getClientId())
                .build()

            val res = notifyStubBlocking.unSubscribe(request)
            printlnCK("unsubscribeNotificationChannel, ${res.success}")
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("unsubscribeNotificationChannel, $e")
            return@withContext false
        }
    }

    private fun listenMessageChannel() {
        val ourClientId = getClientId()
        printlnCK("listenMessageChannel: $ourClientId")
        val request = MessageOuterClass.ListenRequest.newBuilder()
            .setClientId(ourClientId)
            .build()

        messageHandler = MessageHandler()

        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        MessageGrpc.newStub(channel).listen(request, messageHandler)
    }

    private fun listenNotificationChannel() {
        val ourClientId = getClientId()
        printlnCK("listenNotificationChannel: $ourClientId")
        val request = NotifyOuterClass.ListenRequest.newBuilder()
            .setClientId(ourClientId)
            .build()

        notificationHandler = NotificationHandler()

        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        NotifyGrpc.newStub(channel).listen(request, notificationHandler)
    }

    suspend fun sendMessageInPeer(receiverId: String, groupId: Long, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageInPeer: plainMessage = $plainMessage, groupId= $groupId")
        try {
            val signalProtocolAddress = SignalProtocolAddress(receiverId, 111)

            /*if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(receiverId, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return@withContext false
                }
            }*/
            val initSuccess = initSessionUserPeer(receiverId, signalProtocolAddress, clientBlocking, signalProtocolStore)
            if (!initSuccess) {
                return@withContext false
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
            saveNewMessage(response, plainMessage)

            printlnCK("send message success: $plainMessage")
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun sendMessageToGroup(groupId: Long, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId")
        try {
            val senderAddress = SignalProtocolAddress(senderId, 111)
            val groupSender  =  SenderKeyName(groupId.toString(), senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.name)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            val response = messageBlockingGrpc.publish(request)
            saveNewMessage(response, plainMessage)

            printlnCK("send message success: $plainMessage")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

    private suspend fun decryptMessageFromPeer(value: MessageOuterClass.MessageObjectResponse) {
        try {
            val plainMessage = decryptPeerMessage(value.fromClientId, value.message, signalProtocolStore)
            saveNewMessage(value, plainMessage)

            printlnCK("decryptMessageFromPeer: $plainMessage")
        } catch (e: Exception) {
            saveNewMessage(value, getUnableErrorMessage(e.message))
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    private suspend fun decryptMessageFromGroup(value: MessageOuterClass.MessageObjectResponse) {
        try {
            val plainMessage = decryptGroupMessage(value.fromClientId, value.groupId, value.message, senderKeyStore, clientBlocking)
            saveNewMessage(value, plainMessage)

            printlnCK("decryptMessageFromGroup: $plainMessage")
        } catch (e: Exception) {
            saveNewMessage(value, getUnableErrorMessage(e.message))
            printlnCK("decryptMessageFromGroup error : $e")
        }
    }

    private suspend fun saveNewMessage(value: MessageOuterClass.MessageObjectResponse, messageString: String) {
        val groupId = value.groupId
        var room: ChatGroup? = groupRepository.getGroupByID(groupId)
        if (room == null) {
            room = groupRepository.getGroupFromAPI(groupId)
            if (room != null) {
                groupRepository.insertGroup(room)
            }
        }

        if (room == null) {
            printlnCK("insertNewMessage error: can not a room with id $groupId")
            return
        }

        val messageRecord = convertMessageResponse(value, messageString)
        messageDAO.insert(messageRecord)

        // update last message in room
        val updateRoom = ChatGroup(
                id = room.id,
                groupName = room.groupName,
                groupAvatar = room.groupAvatar,
                groupType = room.groupType,
                createBy = room.createBy,
                createdAt = room.createdAt,
                updateBy = value.fromClientId,
                updateAt = getCurrentDateTime().time,
                rtcToken = room.rtcToken,
                clientList = room.clientList,

                // update
                isJoined = room.isJoined,

                lastMessage = messageRecord,
                lastMessageAt = messageRecord.createdTime,
            lastMessageSyncTimestamp = room.lastMessageSyncTimestamp
        )
        groupRepository.updateRoom(updateRoom)
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

    inner class MessageHandler : ClientCallStreamObserver<MessageOuterClass.MessageObjectResponse>() {

        override fun isReady(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setOnReadyHandler(onReadyHandler: Runnable?) {
            TODO("Not yet implemented")
        }

        override fun disableAutoInboundFlowControl() {
            TODO("Not yet implemented")
        }

        override fun request(count: Int) {
            TODO("Not yet implemented")
        }

        override fun setMessageCompression(enable: Boolean) {
            TODO("Not yet implemented")
        }

        override fun cancel(message: String?, cause: Throwable?) {
            printlnCK("MessageHandler, canceled: $message")
        }

        override fun onNext(value: MessageOuterClass.MessageObjectResponse) {
            printlnCK("listenMessageChannel, Receive a message from : ${value.fromClientId}" +
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

        override fun onError(t: Throwable) {
            isNeedSubscribeMessageAgain = true
            printlnCK("Listen message error: ${t.toString()}")
        }

        override fun onCompleted() {
            printlnCK("listenMessageChannel, listen success")
            isNeedSubscribeMessageAgain = false
        }
    }

    inner class NotificationHandler : ClientCallStreamObserver<NotifyOuterClass.NotifyObjectResponse>() {

        override fun isReady(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setOnReadyHandler(onReadyHandler: Runnable?) {
            TODO("Not yet implemented")
        }

        override fun disableAutoInboundFlowControl() {
            TODO("Not yet implemented")
        }

        override fun request(count: Int) {
            TODO("Not yet implemented")
        }

        override fun setMessageCompression(enable: Boolean) {
            TODO("Not yet implemented")
        }

        override fun cancel(message: String?, cause: Throwable?) {
            printlnCK("NotificationHandler, canceled: $message")
        }

        override fun onNext(value: NotifyOuterClass.NotifyObjectResponse) {
            printlnCK("listenNotificationChannel, Receive a notification from : ${value.refClientId}" +
                    ", groupId = ${value.refGroupId} groupType = ${value.notifyType}")
            scope.launch {
                if(value.notifyType == "new-group") {
                    groupRepository.fetchRoomsFromAPI()
                }
            }
        }

        override fun onError(t: Throwable) {
            isNeedSubscribeNotificationAgain = true
            printlnCK("Listen notification error: $t")
        }

        override fun onCompleted() {
            printlnCK("listenNotificationChannel, listen success")
            isNeedSubscribeNotificationAgain = false
        }
    }
}
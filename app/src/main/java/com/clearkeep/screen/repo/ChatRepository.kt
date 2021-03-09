package com.clearkeep.screen.repo

import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.getUnableErrorMessage
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
        // network calls
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        private val notifyStub: NotifyGrpc.NotifyStub,
        private val messageGrpc: MessageGrpc.MessageStub,
        private val messageBlockingGrpc: MessageGrpc.MessageBlockingStub,

        // data
        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
        private val userManager: UserManager,
        private val messageDAO: MessageDAO,

        private val groupRepository: GroupRepository,
        private val messageRepository: MessageRepository,
) {
    fun initSubscriber() {
        subscribeMessageChannel()
        subscribeNotificationChannel()
    }

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var roomId: Long = -1

    private var isNeedSubscribeMessageAgain = false

    private var isNeedSubscribeNotificationAgain = false


    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getClientId() = userManager.getClientId()

    fun reInitSubscriber() {
        printlnCK("reInitSubscriber, isNeedSubscribeMessageAgain = $isNeedSubscribeMessageAgain" +
                ", isNeedSubscribeNotificationAgain = $isNeedSubscribeNotificationAgain")
        if (isNeedSubscribeMessageAgain) {
            subscribeMessageChannel()
        }
        if (isNeedSubscribeNotificationAgain) {
            subscribeNotificationChannel()
        }
        if (isNeedSubscribeMessageAgain || isNeedSubscribeNotificationAgain) {
            scope.launch {
                groupRepository.fetchRoomsFromAPI()
                if (roomId > 0) {
                    val group = groupRepository.getGroupByID(roomId)!!
                    messageRepository.updateMessageFromAPI(group.id, group.lastMessageSyncTimestamp)
                }
            }
        }
    }

    suspend fun sendMessageInPeer(receiverId: String, groupId: Long, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
        val senderId = getClientId()
        printlnCK("sendMessageInPeer: sender=$senderId, receiver= $receiverId, groupId= $groupId")
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
        } catch (e: java.lang.Exception) {
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

    private fun subscribeMessageChannel() {
        val ourClientId = getClientId()
        printlnCK("subscribeMessageChannel: $ourClientId")
        val request = MessageOuterClass.SubscribeRequest.newBuilder()
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
        val ourClientId = getClientId()
        printlnCK("listenMessageChannel: $ourClientId")
        val request = MessageOuterClass.ListenRequest.newBuilder()
                .setClientId(ourClientId)
                .build()
        messageGrpc.listen(request, object : StreamObserver<MessageOuterClass.MessageObjectResponse> {
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

            override fun onError(t: Throwable?) {
                printlnCK("Listen message error: ${t.toString()}")
                isNeedSubscribeMessageAgain = true
            }

            override fun onCompleted() {
                isNeedSubscribeMessageAgain = false
            }
        })
    }

    private fun subscribeNotificationChannel() {
        printlnCK("subscribeNotificationChannel")
        val ourClientId = getClientId()
        val request = NotifyOuterClass.SubscribeRequest.newBuilder()
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
        val ourClientId = getClientId()
        printlnCK("listenNotificationChannel: $ourClientId")
        val request = NotifyOuterClass.ListenRequest.newBuilder()
                .setClientId(ourClientId)
                .build()
        notifyStub.listen(request, object : StreamObserver<NotifyOuterClass.NotifyObjectResponse> {
            override fun onNext(value: NotifyOuterClass.NotifyObjectResponse) {
                value.createdAt
                printlnCK("listenNotificationChannel, Receive a notification from : ${value.refClientId}" +
                        ", groupId = ${value.refGroupId} groupType = ${value.notifyType}")
                scope.launch {
                    if(value.notifyType == "new-group") {
                        groupRepository.fetchRoomsFromAPI()
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen notification error: ${t.toString()}")
                isNeedSubscribeNotificationAgain = true
            }

            override fun onCompleted() {
                isNeedSubscribeNotificationAgain = false
            }
        })
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
}
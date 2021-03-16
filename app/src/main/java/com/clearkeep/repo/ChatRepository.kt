package com.clearkeep.repo

import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import message.MessageGrpc
import message.MessageOuterClass
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

    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getJoiningRoomId() : Long {
        return roomId
    }

    fun getClientId() = userManager.getClientId()

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

    suspend fun decryptMessageFromPeer(value: MessageOuterClass.MessageObjectResponse) {
        try {
            val plainMessage = decryptPeerMessage(value.fromClientId, value.message, signalProtocolStore)
            saveNewMessage(value, plainMessage)

            printlnCK("decryptMessageFromPeer: $plainMessage")
        } catch (e: Exception) {
            saveNewMessage(value, getUnableErrorMessage(e.message))
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    suspend fun decryptMessageFromGroup(value: MessageOuterClass.MessageObjectResponse) {
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
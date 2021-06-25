package com.clearkeep.repo

import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.decryptGroupMessage
import com.clearkeep.screen.chat.utils.decryptPeerMessage
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.getUnableErrorMessage
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import org.whispersystems.libsignal.DuplicateMessageException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    // dao
    private val groupDAO: GroupDAO,
    private val messageDAO: MessageDAO,

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
) {
    fun getMessagesAsState(groupId: Long) = messageDAO.getMessagesAsState(groupId)

    suspend fun getMessages(groupId: Long) = messageDAO.getMessages(groupId)

    suspend fun getMessage(messageId: String) = messageDAO.getMessage(messageId)

    suspend fun getUnreadMessage(groupId: Long, ourClientId: String) : List<Message> {
        val group = groupDAO.getGroupById(groupId)!!
        return messageDAO.getMessagesAfterTime(groupId, group.lastMessageSyncTimestamp).dropWhile { it.senderId ==  ourClientId}
    }

    suspend fun insert(message: Message) = messageDAO.insert(message)

    suspend fun updateMessageFromAPI(groupId: Long, lastMessageAt: Long, offSet: Int = 0) = withContext(Dispatchers.IO) {
        try {
            val request = MessageOuterClass.GetMessagesInGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .setOffSet(offSet)
                    .setLastMessageAt(lastMessageAt)
                    .build()
            val responses = dynamicAPIProvider.provideMessageBlockingStub().getMessagesInGroup(request)
            val messages = responses.lstMessageList.map { parseMessageResponse(it) }
            if (messages.isNotEmpty()) {
                messageDAO.insertMessages(messages)
                val lastMessage = messages.maxByOrNull { it.createdTime }
                if (lastMessage != null) {
                    updateLastSyncMessageTime(groupId, lastMessage)
                }
            }
        } catch (exception: Exception) {
            printlnCK("fetchMessageFromAPI: $exception")
        }
    }

    private suspend fun updateLastSyncMessageTime(groupId: Long, lastMessage: Message) {
        printlnCK("updateLastSyncMessageTime, groupId = $groupId")
        val group = groupDAO.getGroupById(groupId)!!
        val updateGroup = ChatGroup(
            id = group.id,
            groupName = group.groupName,
            groupAvatar = group.groupAvatar,
            groupType = group.groupType,
            createBy = group.createBy,
            createdAt = group.createdAt,
            updateBy = group.updateBy,
            updateAt = group.updateAt,
            rtcToken = group.rtcToken,
            clientList = group.clientList,
            isJoined = group.isJoined,
            lastMessage = lastMessage,
            lastMessageAt = lastMessage.createdTime,
            // update
            lastMessageSyncTimestamp = lastMessage.createdTime
        )
        groupDAO.update(updateGroup)
    }

    private suspend fun parseMessageResponse(messageResponse: MessageOuterClass.MessageObjectResponse): Message {
        val oldMessage = messageDAO.getMessage(messageResponse.id)
        if (oldMessage != null) {
            return oldMessage
        }
        val decryptedMessage = try {
            val result = if (!isGroup(messageResponse.groupType)) {
                decryptPeerMessage(messageResponse.fromClientId, messageResponse.message, signalProtocolStore)
            } else {
                decryptGroupMessage(messageResponse.fromClientId, messageResponse.groupId,
                        messageResponse.message, senderKeyStore, dynamicAPIProvider.provideSignalKeyDistributionBlockingStub())
            }
            printlnCK("parseMessageResponse, success: $result")
            result
        } catch (e: DuplicateMessageException) {
            printlnCK("parseMessageResponse, error: $e")
            /**
             * To fix case: both load message and receive message from socket at the same time
             * Need wait 1.5s to load old message before save unableDecryptMessage
             */
            delay(1500)
            val oldMessage = messageDAO.getMessage(messageResponse.id)
            oldMessage?.message ?: getUnableErrorMessage(e.message)
        } catch (e: Exception) {
            printlnCK("parseMessageResponse, error: id= ${messageResponse.id}, group id = ${messageResponse.groupId} type= ${messageResponse.groupType}, error : $e")
            getUnableErrorMessage(e.message)
        }
        return Message(
            messageResponse.id,
            messageResponse.groupId,
            messageResponse.groupType,
            messageResponse.fromClientId,
            messageResponse.clientId,
            decryptedMessage,
            messageResponse.createdAt,
            messageResponse.updatedAt,
        )
    }
}
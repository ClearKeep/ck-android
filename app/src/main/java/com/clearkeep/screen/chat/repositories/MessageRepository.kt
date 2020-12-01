package com.clearkeep.screen.chat.repositories

import com.clearkeep.db.MessageDAO
import com.clearkeep.db.model.Message
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.decryptGroupMessage
import com.clearkeep.screen.chat.utils.decryptPeerMessage
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageGrpc
import message.MessageOuterClass
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
        private val messageDAO: MessageDAO,
        private val messageGrpc: MessageGrpc.MessageBlockingStub,

        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,

        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
) {
    fun getMessagesAsState(groupId: String) = messageDAO.getMessagesAsState(groupId)

    suspend fun getMessages(groupId: String) = messageDAO.getMessages(groupId)

    suspend fun getMessage(messageId: String) = messageDAO.getMessage(messageId)

    suspend fun insert(message: Message) = messageDAO.insert(message)

    suspend fun fetchMessageFromAPI(groupId: String, lastMessageAt: Long, offSet: Int = 0) = withContext(Dispatchers.IO) {
        try {
            val request = MessageOuterClass.GetMessagesInGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .setOffSet(offSet)
                    .setLastMessageAt(lastMessageAt)
                    .build()
            val responses = messageGrpc.getMessagesInGroup(request)
            messageDAO.insertMessages(responses.lstMessageList.mapNotNull { convertMessageResponse(it) })
        } catch(exception: Exception) {
            printlnCK("fetchMessageFromAPI: $exception")
        }
    }

    private suspend fun convertMessageResponse(messageResponse: MessageOuterClass.MessageObjectResponse) : Message? {
        return try {
            val decryptedMessage = if (!isGroup(messageResponse.groupType)) {
                decryptPeerMessage(messageResponse.fromClientId, messageResponse.message, signalProtocolStore)
            } else {
                decryptGroupMessage(messageResponse.fromClientId, messageResponse.groupId, messageResponse.message, senderKeyStore, clientBlocking)
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
        } catch (e: Exception) {
            printlnCK("MessageRepository: convertMessageResponse error : $e")
            return null
        }
    }
}
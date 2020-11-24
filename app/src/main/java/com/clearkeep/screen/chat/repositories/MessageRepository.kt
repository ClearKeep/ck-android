package com.clearkeep.screen.chat.repositories

import com.clearkeep.db.MessageDAO
import com.clearkeep.db.model.Message
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.decryptGroupMessage
import com.clearkeep.screen.chat.utils.decryptPeerMessage
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
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
    fun getMessages(groupId: String) = messageDAO.getMessages(groupId)

    suspend fun insert(message: Message) = messageDAO.insert(message)

    suspend fun fetchMessageToStore(groupId: String) = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.GetMessagesInGroupRequest.newBuilder()
                .setGroupId(groupId)
                .setOffSet(0)
                .setLastMessageAt(0)
                .build()
        val responses = messageGrpc.getMessagesInGroup(request)
        messageDAO.deleteMessageFromGroupId(groupId)
        messageDAO.insertMessages(responses.lstMessageList.map { convertMessageResponse(it) })
    }

    private suspend fun convertMessageResponse(messageResponse: MessageOuterClass.MessageObjectResponse) : Message {
        return Message(
                senderId = messageResponse.fromClientId,
                message = decryptMessage(messageResponse.fromClientId, messageResponse.message),
                groupId = messageResponse.groupId,
                createdTime = messageResponse.createdAt,
                updatedTime = messageResponse.updatedAt,
                receiverId = messageResponse.clientId,
        )
    }

    private suspend fun decryptMessage(fromClientId: String, message: ByteString): String {
        printlnCK("get message from $fromClientId")
        return try {
            decryptPeerMessage(fromClientId, message, signalProtocolStore)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
            "encrypt error"
        }
    }

    /*private fun decryptMessageFromGroup(fromClientId: String, groupId: String, message: String): String {
        return try {
            decryptGroupMessage(fromClientId, groupId, message, senderKeyStore, clientBlocking)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromGroup error : $e")
            "encrypt error"
        }
    }*/
}
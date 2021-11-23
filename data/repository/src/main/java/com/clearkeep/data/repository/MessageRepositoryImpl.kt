package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import com.clearkeep.data.remote.service.MessageService
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.domain.model.*
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import org.whispersystems.libsignal.*
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDAO: MessageDAO,
    private val messageService: MessageService,
): MessageRepository {
    override fun getMessagesAsState(groupId: Long, owner: Owner) =
        messageDAO.getMessagesAsState(groupId, owner.domain, owner.clientId)

    override suspend fun getUnreadMessage(
        groupId: Long,
        lastMessageSyncTimestamp: Long,
        domain: String,
        ourClientId: String
    ): List<Message> = withContext(Dispatchers.IO) {
        return@withContext messageDAO.getMessagesAfterTime(
            groupId,
            lastMessageSyncTimestamp,
            domain,
            ourClientId
        )
    }

    override suspend fun updateMessageFromAPI(server: Server, groupId: Long, owner: Owner, lastMessageAt: Long, loadSize: Int): MessageOuterClass.GetMessagesInGroupResponse? = withContext(Dispatchers.IO) {
        try {
            return@withContext messageService.getMessage(server, groupId, loadSize, lastMessageAt)
        } catch (e: StatusRuntimeException) {
            return@withContext null
        } catch (exception: Exception) {
            return@withContext null
        }
    }

    override fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<Message>> {
        return messageDAO.getMessageByText(ownerDomain, ownerClientId, "%$query%")
    }

    override suspend fun getGroupMessage(messageId: String, groupId: Long): Message? = withContext(Dispatchers.IO) {
        return@withContext messageDAO.getMessage(messageId, groupId)
    }

    override suspend fun clearTempMessage() {
        withContext(Dispatchers.IO) {
            messageDAO.deleteTempMessages()
        }
    }

    override suspend fun saveMessage(message: Message): Int = withContext(Dispatchers.IO) {
        return@withContext messageDAO.insert(message).toInt()
    }

    override suspend fun saveMessages(messages: List<Message>) = withContext(Dispatchers.IO) {
        messageDAO.insertMessages(messages)
    }

    override suspend fun deleteMessageInGroup(groupId: Long, ownerDomain: String, ownerClientId: String) = withContext(Dispatchers.IO) {
        messageDAO.deleteMessageFromGroupId(groupId, ownerDomain, ownerClientId)
    }

    override suspend fun deleteMessageByDomain(domain: String, userId: String) = withContext(Dispatchers.IO) {
        messageDAO.deleteMessageByDomain(domain, userId)
    }
}

data class MessagePagingResponse(
    val isSuccess: Boolean,
    val endOfPaginationReached: Boolean,
    val newestMessageLoadedTimestamp: Long
)
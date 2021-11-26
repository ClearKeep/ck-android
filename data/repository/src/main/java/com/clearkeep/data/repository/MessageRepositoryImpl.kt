package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.data.remote.service.MessageService
import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.local.model.toLocal
import com.clearkeep.data.remote.utils.toEntity
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GetMessagesInGroupResponse
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.*
import java.util.*
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDAO: MessageDAO,
    private val messageService: MessageService,
) : MessageRepository {
    override fun getMessagesAsState(groupId: Long, owner: Owner) =
        messageDAO.getMessagesAsState(groupId, owner.domain, owner.clientId)
            .map { it.map { it.toEntity() } }

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
        ).map { it.toEntity() }
    }

    override suspend fun updateMessageFromAPI(
        server: Server,
        groupId: Long,
        owner: Owner,
        lastMessageAt: Long,
        loadSize: Int
    ): GetMessagesInGroupResponse? {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext messageService.getMessage(
                    server,
                    groupId,
                    loadSize,
                    lastMessageAt
                ).toEntity()
            } catch (e: StatusRuntimeException) {
                return@withContext null
            } catch (exception: Exception) {
                return@withContext null
            }
        }
    }

    override fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<Message>> {
        return messageDAO.getMessageByText(ownerDomain, ownerClientId, "%$query%")
            .map { it.map { it.toEntity() } }
    }

    override suspend fun getGroupMessage(messageId: String, groupId: Long): Message? =
        withContext(Dispatchers.IO) {
            return@withContext messageDAO.getMessage(messageId, groupId)?.toEntity()
        }

    override suspend fun clearTempMessage() {
        withContext(Dispatchers.IO) {
            messageDAO.deleteTempMessages()
        }
    }

    override suspend fun saveMessage(message: Message): Int = withContext(Dispatchers.IO) {
        return@withContext messageDAO.insert(message.toLocal()).toInt()
    }

    override suspend fun saveMessages(messages: List<Message>) = withContext(Dispatchers.IO) {
        messageDAO.insertMessages(messages.map { it.toLocal() })
    }

    override suspend fun deleteMessageInGroup(
        groupId: Long,
        ownerDomain: String,
        ownerClientId: String
    ) = withContext(Dispatchers.IO) {
        messageDAO.deleteMessageFromGroupId(groupId, ownerDomain, ownerClientId)
    }

    override suspend fun deleteMessageByDomain(domain: String, userId: String) =
        withContext(Dispatchers.IO) {
            messageDAO.deleteMessageByDomain(domain, userId)
        }
}
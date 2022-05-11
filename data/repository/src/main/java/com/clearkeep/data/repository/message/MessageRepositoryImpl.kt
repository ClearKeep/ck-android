package com.clearkeep.data.repository.message

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.remote.service.MessageService
import com.clearkeep.data.local.clearkeep.message.MessageDAO
import com.clearkeep.data.repository.group.toEntity
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GetMessagesInGroupResponse
import com.clearkeep.domain.model.response.MessageObjectResponse
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
            .map { it.map { it.toModel() } }

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
        ).map { it.toModel() }
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
            .map { it.map { it.toModel() } }
    }

    override suspend fun getGroupMessage(messageId: String, groupId: Long): Message? =
        withContext(Dispatchers.IO) {
            return@withContext messageDAO.getMessage(messageId, groupId)?.toModel()
        }

    override suspend fun clearTempMessage() {
        withContext(Dispatchers.IO) {
            messageDAO.deleteTempMessages()
        }
    }

    override suspend fun saveMessage(message: Message): Int = withContext(Dispatchers.IO) {
        return@withContext messageDAO.insert(message.toEntity()).toInt()
    }

    override suspend fun saveMessages(messages: List<Message>) = withContext(Dispatchers.IO) {
        messageDAO.insertMessages(messages.map { it.toEntity() })
    }

    override suspend fun deleteMessageInGroup(
        groupId: Long,
        ownerDomain: String,
        ownerClientId: String
    ):Int = withContext(Dispatchers.IO) {
       val test= messageDAO.deleteMessageFromGroupId(groupId, ownerDomain, ownerClientId)
        Log.d("antx: ", "MessageRepositoryImpl deleteMessageInGroup line = 101: $test " );
        return@withContext test
    }

    override suspend fun deleteMessageByDomain(domain: String, userId: String): Int  =
        withContext(Dispatchers.IO) {
          val test=  messageDAO.deleteMessageByDomain(domain, userId)
            Log.d("antx: ", "MessageRepositoryImpl deleteMessageByDomain line = 109: $test" )
            return@withContext test
        }

    private var roomId: Long = -1

    override fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    override fun getJoiningRoomId(): Long {
        return roomId
    }

    override suspend fun sendMessageInPeer(
        server: Server,
        receiverClientId: String,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
        messageSender: ByteArray
    ): Resource<MessageObjectResponse> = withContext(Dispatchers.IO) {
        try {
            val response = messageService.sendMessagePeer(server, receiverClientId, deviceId, groupId, message, messageSender)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            return@withContext Resource.error(parsedError.message, null, parsedError.code, parsedError.cause)
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun sendMessageToGroup(
        server: Server,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
    ): Resource<MessageObjectResponse> = withContext(Dispatchers.IO) {
        try {
            val response = messageService.sendMessageGroup(server, deviceId, groupId, message)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            return@withContext Resource.error(parsedError.message, null, parsedError.code, parsedError.cause)
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun updateMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDAO.updateMessage(message.toEntity())
    }
}
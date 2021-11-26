package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.remote.service.*
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.model.toLocal
import com.clearkeep.data.remote.utils.toEntity
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.response.MessageObjectResponse
import com.clearkeep.domain.model.Server
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val messageDAO: MessageDAO,
    private val messageService: MessageService
) : ChatRepository {
    private var roomId: Long = -1

    override fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    override fun getJoiningRoomId(): Long {
        return roomId
    }

    override suspend fun sendMessageInPeer(
        server: com.clearkeep.domain.model.Server,
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
        messageDAO.updateMessage(message.toLocal())
    }
}
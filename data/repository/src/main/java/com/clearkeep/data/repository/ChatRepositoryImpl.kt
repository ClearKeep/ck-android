package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.remote.service.*
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.presentation.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.clearkeep.common.utilities.network.Resource
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import message.MessageOuterClass
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
    ): com.clearkeep.common.utilities.network.Resource<MessageOuterClass.MessageObjectResponse> = withContext(Dispatchers.IO) {
        try {
            val response = messageService.sendMessagePeer(server, receiverClientId, deviceId, groupId, message, messageSender)
            return@withContext com.clearkeep.common.utilities.network.Resource.success(response)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            return@withContext com.clearkeep.common.utilities.network.Resource.error(parsedError.message, null, parsedError.code, parsedError.cause)
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext com.clearkeep.common.utilities.network.Resource.error(e.toString(), null)
        }
    }

    override suspend fun sendMessageToGroup(
        server: com.clearkeep.domain.model.Server,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
    ): com.clearkeep.common.utilities.network.Resource<MessageOuterClass.MessageObjectResponse> = withContext(Dispatchers.IO) {
        try {
            val response = messageService.sendMessageGroup(server, deviceId, groupId, message)
            return@withContext com.clearkeep.common.utilities.network.Resource.success(response)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            return@withContext com.clearkeep.common.utilities.network.Resource.error(parsedError.message, null, parsedError.code, parsedError.cause)
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
            return@withContext com.clearkeep.common.utilities.network.Resource.error(e.toString(), null)
        }
    }

    override suspend fun updateMessage(message: com.clearkeep.domain.model.Message) = withContext(Dispatchers.IO) {
        messageDAO.updateMessage(message)
    }
}
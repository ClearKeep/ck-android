package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.Server
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.MessageObjectResponse

interface ChatRepository {
    fun setJoiningRoomId(roomId: Long)
    fun getJoiningRoomId(): Long
    suspend fun sendMessageInPeer(
        server: Server,
        receiverClientId: String,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
        messageSender: ByteArray
    ): Resource<MessageObjectResponse>

    suspend fun sendMessageToGroup(
        server: Server,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
    ): Resource<MessageObjectResponse>

    suspend fun updateMessage(message: Message)
}
package com.clearkeep.data.remote.service

import com.clearkeep.domain.model.Server
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import javax.inject.Inject

class MessageService @Inject constructor(
    private val paramAPIProvider: ParamAPIProvider,
) {
    suspend fun sendMessagePeer(server: com.clearkeep.domain.model.Server, receiverClientId: String, deviceId: String, groupId: Long, message: ByteArray, messageSender: ByteArray) = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.PublishRequest.newBuilder()
            .setGroupId(groupId)
            .setFromClientDeviceId(deviceId)
            .setClientId(receiverClientId)
            .setMessage(ByteString.copyFrom(message))
            .setSenderMessage(ByteString.copyFrom(messageSender))
            .build()

        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        return@withContext paramAPIProvider.provideMessageBlockingStub(paramAPI).publish(request)
    }
    
    suspend fun sendMessageGroup(
        server: com.clearkeep.domain.model.Server,
        deviceId: String,
        groupId: Long,
        message: ByteArray,
    ): MessageOuterClass.MessageObjectResponse = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.PublishRequest.newBuilder()
            .setGroupId(groupId)
            .setFromClientDeviceId(deviceId)
            .setMessage(ByteString.copyFrom(message))
            .setSenderMessage(ByteString.copyFrom(message))
            .build()

        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        return@withContext paramAPIProvider.provideMessageBlockingStub(paramAPI).publish(request)
    }
    
    suspend fun getMessage(server: com.clearkeep.domain.model.Server, groupId: Long, loadSize: Int, lastMessageAt: Long): MessageOuterClass.GetMessagesInGroupResponse = withContext(Dispatchers.IO) {
        val messageGrpc = paramAPIProvider.provideMessageBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
        val request = MessageOuterClass.GetMessagesInGroupRequest.newBuilder()
            .setGroupId(groupId)
            .setOffSet(loadSize)
            .setLastMessageAt(lastMessageAt)
            .build()

        return@withContext messageGrpc.getMessagesInGroup(request)
    }
}
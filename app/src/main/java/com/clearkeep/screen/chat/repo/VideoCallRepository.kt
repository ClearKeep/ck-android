package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.*
import com.clearkeep.utilities.CALL_TYPE_VIDEO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video_call.VideoCallOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCallRepository @Inject constructor(
    // network calls
    private val apiProvider: ParamAPIProvider,

    private val serverRepository: ServerRepository
) {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean, owner: Owner) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        printlnCK("requestVideoCall: groupId = $groupId, ${owner.domain}, ${owner.clientId}")
        val server = serverRepository.getServer(owner.domain, owner.clientId)
        if (server == null) {
            printlnCK("requestVideoCall: Can not find server: ${owner.domain} + ${owner.clientId}")
            return@withContext null
        }
        try {
            val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setCallType(if (isAudioMode) CALL_TYPE_AUDIO else CALL_TYPE_VIDEO)
                .build()
            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
            return@withContext videoCallGrpc.videoCall(request)
        } catch (e: Exception) {
            printlnCK("requestVideoCall: $e")
            return@withContext null
        }
    }

    suspend fun cancelCall(groupId: Int, owner: Owner) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("cancelCall: groupId = $groupId, ${owner.domain}, ${owner.clientId}")
        val server = serverRepository.getServer(owner.domain, owner.clientId)
        if (server == null) {
            printlnCK("cancelCall: Can not find server: ${owner.domain} + ${owner.clientId}")
            return@withContext false
        }
        try {
            val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setUpdateType(CALL_UPDATE_TYPE_CANCEL)
                .build()
            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
            val success = videoCallGrpc.updateCall(request).success
            printlnCK("cancelCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("cancelCall: $e")
            return@withContext false
        }
    }

    suspend fun switchAudioToVideoCall(groupId: Int, owner: Owner) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("switchAudioToVideoCall: groupId = $groupId, ${owner.domain}, ${owner.clientId}")
        val server = serverRepository.getServer(owner.domain, owner.clientId)
        if (server == null) {
            printlnCK("switchAudioToVideoCall: Can not find server: ${owner.domain} + ${owner.clientId}")
            return@withContext false
        }
        try {
            val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setUpdateType(CALL_TYPE_VIDEO)
                .build()
            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
            val success = videoCallGrpc.updateCall(request).success
            printlnCK("switchAudioToVideoCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("switchAudioToVideoCall: $e")
            return@withContext false
        }
    }
}
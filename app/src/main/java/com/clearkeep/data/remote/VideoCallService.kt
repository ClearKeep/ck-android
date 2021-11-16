package com.clearkeep.data.remote

import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.utilities.CALL_TYPE_AUDIO
import com.clearkeep.utilities.CALL_TYPE_VIDEO
import com.clearkeep.utilities.CALL_UPDATE_TYPE_BUSY
import com.clearkeep.utilities.CALL_UPDATE_TYPE_CANCEL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video_call.VideoCallOuterClass
import javax.inject.Inject

class VideoCallService @Inject constructor(
    private val apiProvider: ParamAPIProvider,
) {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean, server: Server) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setCallType(if (isAudioMode) CALL_TYPE_AUDIO else CALL_TYPE_VIDEO)
            .build()

        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)

        return@withContext apiProvider.provideVideoCallBlockingStub(paramAPI).videoCall(request)
    }

    suspend fun cancelCall(groupId: Int, server: Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_UPDATE_TYPE_CANCEL)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }

    suspend fun setBusy(groupId: Int, server: Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_UPDATE_TYPE_BUSY)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }

    suspend fun switchFromAudioToVideoCall(groupId: Int, server: Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_TYPE_VIDEO)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }
}
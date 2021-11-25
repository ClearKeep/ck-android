package com.clearkeep.data.remote.service

import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.clearkeep.common.utilities.CALL_TYPE_AUDIO
import com.clearkeep.common.utilities.CALL_TYPE_VIDEO
import com.clearkeep.common.utilities.CALL_UPDATE_TYPE_BUSY
import com.clearkeep.common.utilities.CALL_UPDATE_TYPE_CANCEL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video_call.VideoCallOuterClass
import javax.inject.Inject

class VideoCallService @Inject constructor(
    private val apiProvider: ParamAPIProvider,
) {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean, server: com.clearkeep.domain.model.Server) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setCallType(if (isAudioMode) CALL_TYPE_AUDIO else CALL_TYPE_VIDEO)
            .build()

        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)

        return@withContext apiProvider.provideVideoCallBlockingStub(paramAPI).videoCall(request)
    }

    suspend fun cancelCall(groupId: Int, server: com.clearkeep.domain.model.Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_UPDATE_TYPE_CANCEL)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }

    suspend fun setBusy(groupId: Int, server: com.clearkeep.domain.model.Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_UPDATE_TYPE_BUSY)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }

    suspend fun switchFromAudioToVideoCall(groupId: Int, server: com.clearkeep.domain.model.Server): VideoCallOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
            .setGroupId(groupId.toLong())
            .setUpdateType(CALL_TYPE_VIDEO)
            .build()
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val videoCallGrpc = apiProvider.provideVideoCallBlockingStub(paramAPI)
        return@withContext videoCallGrpc.updateCall(request)
    }
}
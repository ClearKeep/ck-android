package com.clearkeep.repo

import com.clearkeep.dynamicapi.DynamicAPIProvider
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
    private val dynamicAPIProvider: DynamicAPIProvider,
) {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        printlnCK("requestVideoCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setCallType(if (isAudioMode) CALL_TYPE_AUDIO else CALL_TYPE_VIDEO)
                .build()
            return@withContext dynamicAPIProvider.provideVideoCallBlockingStub().videoCall(request)
        } catch (e: Exception) {
            printlnCK("requestVideoCall: $e")
            return@withContext null
        }
    }

    suspend fun cancelCall(groupId: Int) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("cancelCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setUpdateType(CALL_UPDATE_TYPE_CANCEL)
                .build()
            val success = dynamicAPIProvider.provideVideoCallBlockingStub().updateCall(request).success
            printlnCK("cancelCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("cancelCall: $e")
            return@withContext false
        }
    }

    suspend fun switchAudioToVideoCall(groupId: Int) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("switchAudioToCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.UpdateCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setUpdateType(CALL_TYPE_VIDEO)
                .build()
            val success = dynamicAPIProvider.provideVideoCallBlockingStub().updateCall(request).success
            printlnCK("switchAudioToCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("switchAudioToCall: $e")
            return@withContext false
        }
    }
}
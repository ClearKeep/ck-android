package com.clearkeep.repo

import com.clearkeep.utilities.CALL_TYPE_AUDIO
import com.clearkeep.utilities.CALL_TYPE_VIDEO
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video_call.VideoCallGrpc
import video_call.VideoCallOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCallRepository @Inject constructor(
        // network calls
    private val videoCallBlockingStub: VideoCallGrpc.VideoCallBlockingStub,
    private val userManager: UserManager,
) {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        printlnCK("requestVideoCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setCallType(if (isAudioMode) CALL_TYPE_AUDIO else CALL_TYPE_VIDEO)
                .build()
            return@withContext videoCallBlockingStub.videoCall(request)
        } catch (e: Exception) {
            printlnCK("requestVideoCall: $e")
            return@withContext null
        }
    }

    suspend fun cancelCall(groupId: Int) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("cancelCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
                .setGroupId(groupId.toLong())
                .setClientId(userManager.getClientId())
                .build()
            val success = videoCallBlockingStub.cancelRequestCall(request).success
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
            val success = videoCallBlockingStub.updateCall(request).success
            printlnCK("switchAudioToCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("switchAudioToCall: $e")
            return@withContext false
        }
    }
}
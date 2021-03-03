package com.clearkeep.screen.repo

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
    suspend fun requestVideoCall(groupId: Int) : VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        printlnCK("requestVideoCall: groupId = $groupId")
        try {
            val request = VideoCallOuterClass.VideoCallRequest.newBuilder()
                    .setGroupId(groupId.toLong())
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
}
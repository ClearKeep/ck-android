package com.clearkeep.screen.repo

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
}
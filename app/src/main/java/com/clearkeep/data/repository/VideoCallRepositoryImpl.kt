package com.clearkeep.data.repository

import com.clearkeep.data.local.clearkeep.dao.ServerDAO
import com.clearkeep.data.remote.service.VideoCallService
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.VideoCallRepository
import com.clearkeep.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video_call.VideoCallOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCallRepositoryImpl @Inject constructor(
    private val videoCallService: VideoCallService
): VideoCallRepository {
    override suspend fun requestVideoCall(
        groupId: Int,
        isAudioMode: Boolean,
        server: Server
    ): VideoCallOuterClass.ServerResponse? = withContext(Dispatchers.IO) {
        try {
            return@withContext videoCallService.requestVideoCall(groupId, isAudioMode, server)
        } catch (e: Exception) {
            printlnCK("requestVideoCall: $e")
            return@withContext null
        }
    }

    override suspend fun cancelCall(groupId: Int, server: Server): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = videoCallService.cancelCall(groupId, server)
            val success = response.error.isNullOrEmpty()
            printlnCK("cancelCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("cancelCall: $e")
            return@withContext false
        }
    }

    override suspend fun busyCall(groupId: Int, server: Server): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = videoCallService.setBusy(groupId, server)
            val success = response.error.isNullOrEmpty()
            printlnCK("cancelCall, success = $success")
            return@withContext success
        } catch (e: Exception) {
            printlnCK("cancelCall: $e")
            return@withContext false
        }
    }

    override suspend fun switchAudioToVideoCall(groupId: Int, server: Server): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = videoCallService.switchFromAudioToVideoCall(groupId, server)
                val success = response.error.isNullOrEmpty()
                printlnCK("switchAudioToVideoCall, success = $success")
                return@withContext success
            } catch (e: Exception) {
                printlnCK("switchAudioToVideoCall: $e")
                return@withContext false
            }
        }
}
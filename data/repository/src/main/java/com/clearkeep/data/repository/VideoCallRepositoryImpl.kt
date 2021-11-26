package com.clearkeep.data.repository

import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.remote.service.VideoCallService
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.response.ServerResponse
import com.clearkeep.domain.model.response.StunServer
import com.clearkeep.domain.model.response.TurnServer
import com.clearkeep.domain.repository.VideoCallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoCallRepositoryImpl @Inject constructor(
    private val videoCallService: VideoCallService
) : VideoCallRepository {
    override suspend fun requestVideoCall(
        groupId: Int,
        isAudioMode: Boolean,
        server: Server
    ): ServerResponse? = withContext(Dispatchers.IO) {
        try {
            val rawResponse = videoCallService.requestVideoCall(groupId, isAudioMode, server)
            return@withContext rawResponse?.run {
                ServerResponse(
                    groupRtcUrl,
                    groupRtcId,
                    groupRtcToken,
                    StunServer(stunServer.server, stunServer.port),
                    TurnServer(
                        turnServer.server,
                        turnServer.port,
                        turnServer.type,
                        turnServer.user,
                        turnServer.pwd
                    )
                )
            }
        } catch (e: Exception) {
            printlnCK("requestVideoCall: $e")
            return@withContext null
        }
    }

    override suspend fun cancelCall(groupId: Int, server: Server): Boolean =
        withContext(Dispatchers.IO) {
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

    override suspend fun busyCall(groupId: Int, server: Server): Boolean =
        withContext(Dispatchers.IO) {
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
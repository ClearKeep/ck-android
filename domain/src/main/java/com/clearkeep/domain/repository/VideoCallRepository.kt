package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.ServerResponse

interface VideoCallRepository {
    suspend fun requestVideoCall(
        groupId: Int,
        isAudioMode: Boolean,
        server: Server
    ): ServerResponse?

    suspend fun cancelCall(groupId: Int, server: Server): Boolean
    suspend fun busyCall(groupId: Int, server: Server): Boolean
    suspend fun switchAudioToVideoCall(groupId: Int, server: Server): Boolean
}
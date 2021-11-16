package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Owner
import video_call.VideoCallOuterClass

interface VideoCallRepository {
    suspend fun requestVideoCall(
        groupId: Int,
        isAudioMode: Boolean,
        owner: Owner
    ): VideoCallOuterClass.ServerResponse?

    suspend fun cancelCall(groupId: Int, owner: Owner): Boolean
    suspend fun busyCall(groupId: Int, owner: Owner): Boolean
    suspend fun switchAudioToVideoCall(groupId: Int, owner: Owner): Boolean
}
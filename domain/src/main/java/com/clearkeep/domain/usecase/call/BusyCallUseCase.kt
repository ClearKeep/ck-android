package com.clearkeep.domain.usecase.call

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.VideoCallRepository
import javax.inject.Inject

class BusyCallUseCase @Inject constructor(
    private val videoCallRepository: VideoCallRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(
        groupId: Int,
        owner: Owner
    ): Boolean {
        val server = serverRepository.getServer(owner.domain, owner.clientId) ?: return false
        return videoCallRepository.busyCall(groupId, server)
    }
}
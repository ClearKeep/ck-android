package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class DeleteServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(serverId: Int) = serverRepository.deleteServer(serverId)
}
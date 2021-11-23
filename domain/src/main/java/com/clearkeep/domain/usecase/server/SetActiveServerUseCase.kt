package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class SetActiveServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(server: Server) = serverRepository.setActiveServer(server)
}
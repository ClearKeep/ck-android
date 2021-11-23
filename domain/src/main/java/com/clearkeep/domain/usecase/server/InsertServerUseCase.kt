package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class InsertServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(server: com.clearkeep.domain.model.Server) = serverRepository.insertServer(server)
}
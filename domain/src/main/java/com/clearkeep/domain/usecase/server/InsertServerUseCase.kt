package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InsertServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(server: Server) = withContext(Dispatchers.IO) { serverRepository.insertServer(server) }
}
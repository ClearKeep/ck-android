package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(domain: String, ownerId: String) = withContext(Dispatchers.IO) { serverRepository.getServer(domain, ownerId) }
}
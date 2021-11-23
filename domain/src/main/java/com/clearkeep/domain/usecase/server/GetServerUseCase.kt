package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class GetServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(domain: String, ownerId: String) = serverRepository.getServer(domain, ownerId)
}
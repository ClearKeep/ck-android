package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetServerByDomainUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(url: String) = withContext(Dispatchers.IO) { serverRepository.getServerByDomain(url) }
}
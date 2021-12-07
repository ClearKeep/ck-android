package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetServerByOwnerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(owner: Owner) = withContext(Dispatchers.IO) { serverRepository.getServerByOwner(owner) }
}
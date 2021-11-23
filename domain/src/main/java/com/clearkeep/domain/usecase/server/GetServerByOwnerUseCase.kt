package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class GetServerByOwnerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke(owner: com.clearkeep.domain.model.Owner) = serverRepository.getServerByOwner(owner)
}
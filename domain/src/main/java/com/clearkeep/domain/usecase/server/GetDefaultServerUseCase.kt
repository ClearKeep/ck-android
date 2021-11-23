package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class GetDefaultServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    suspend operator fun invoke() = serverRepository.getDefaultServer()
}
package com.clearkeep.domain.usecase.workspace

import com.clearkeep.domain.repository.WorkSpaceRepository
import javax.inject.Inject

class GetWorkspaceInfoUseCase @Inject constructor(private val workSpaceRepository: WorkSpaceRepository) {
    suspend operator fun invoke(currentDomain: String? = null, domain: String) = workSpaceRepository.getWorkspaceInfo(currentDomain, domain)
}
package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAllPeerGroupByDomainUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(owner: Owner) = withContext(Dispatchers.IO) { groupRepository.getAllPeerGroupByDomain(owner) }
}
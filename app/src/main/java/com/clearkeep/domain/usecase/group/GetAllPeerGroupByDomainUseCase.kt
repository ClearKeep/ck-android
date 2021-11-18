package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetAllPeerGroupByDomainUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(owner: Owner) = groupRepository.getAllPeerGroupByDomain(owner)
}
package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupsByGroupNameUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    operator fun invoke(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ) = groupRepository.getGroupsByGroupName(ownerDomain, ownerClientId, query)
}
package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetListClientInGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long, domain: String) =
        groupRepository.getListClientInGroup(groupId, domain)
}
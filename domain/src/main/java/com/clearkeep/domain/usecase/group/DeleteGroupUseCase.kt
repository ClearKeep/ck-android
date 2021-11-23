package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class DeleteGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long, domain: String, ownerClientId: String) =
        groupRepository.deleteGroup(groupId, domain, ownerClientId)

    suspend operator fun invoke(domain: String, ownerClientId: String) =
        groupRepository.deleteGroup(domain, ownerClientId)
}
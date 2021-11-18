package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long, owner: Owner) = groupRepository.leaveGroup(groupId, owner)
}
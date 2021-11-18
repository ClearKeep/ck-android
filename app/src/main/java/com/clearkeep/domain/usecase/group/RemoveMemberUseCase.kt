package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class RemoveMemberUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(removedUser: User, groupId: Long, owner: Owner) =
        groupRepository.removeMemberInGroup(removedUser, groupId, owner)
}
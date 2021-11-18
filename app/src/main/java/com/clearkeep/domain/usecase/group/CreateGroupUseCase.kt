package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean
    ) = groupRepository.createGroup(createClientId, groupName, participants, isGroup)
}
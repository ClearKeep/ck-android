package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class InviteToGroupUseCase @Inject constructor(private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(
        invitedUsers: List<User>,
        groupId: Long,
        owner: Owner
    ): Resource<ChatGroup> {
        val server = serverRepository.getServer(domain = owner.domain, ownerId = owner.clientId)

        return groupRepository.inviteToGroup(invitedUsers, groupId, server, owner)
    }
}
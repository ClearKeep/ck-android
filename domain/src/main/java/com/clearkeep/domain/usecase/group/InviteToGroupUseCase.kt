package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InviteToGroupUseCase @Inject constructor(private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(
        invitedUsers: List<com.clearkeep.domain.model.User>,
        groupId: Long,
        owner: Owner
    ) = withContext(Dispatchers.IO) {
        val server = serverRepository.getServer(domain = owner.domain, ownerId = owner.clientId)

        invitedUsers.forEach {
            printlnCK("inviteToGroupFromAPIs: ${it.userName}")
            groupRepository.inviteUserToGroup(it, groupId, owner)
        }
        printlnCK("inviteToGroupFromAPIs")
        val group = groupRepository.getGroup(groupId, owner, server)
        group.data?.let { groupRepository.insertGroup(it) }
        return@withContext group
    }
}
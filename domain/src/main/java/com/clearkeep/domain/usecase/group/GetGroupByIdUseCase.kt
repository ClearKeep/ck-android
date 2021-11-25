package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import javax.inject.Inject

class GetGroupByIdUseCase @Inject constructor(private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(groupId: Long, domain: String, ownerId: String): Resource<com.clearkeep.domain.model.ChatGroup>? {
        val server = serverRepository.getServer(domain, ownerId)
        if (server == null) {
            printlnCK("getGroupByID: null server")
            return null
        }
        return groupRepository.getGroupByID(groupId, domain, ownerId, server, true)
    }
}
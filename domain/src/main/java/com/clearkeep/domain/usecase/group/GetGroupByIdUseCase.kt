package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.ChatGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetGroupByIdUseCase @Inject constructor(private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(groupId: Long, domain: String, ownerId: String): Resource<ChatGroup>? = withContext(
        Dispatchers.IO) {
        val server = serverRepository.getServer(domain, ownerId)
        if (server == null) {
            printlnCK("getGroupByID: null server")
            return@withContext null
        }
        return@withContext groupRepository.getGroupByID(groupId, domain, ownerId, server, true)
    }
}
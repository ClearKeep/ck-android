package com.clearkeep.domain.usecase.group

import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val environment: Environment,
) {
    suspend operator fun invoke(
        createClientId: String,
        groupName: String,
        participants: MutableList<com.clearkeep.domain.model.User>,
        isGroup: Boolean
    ): Resource<com.clearkeep.domain.model.ChatGroup> {
        val server = serverRepository.getServer(getDomain(), getClientId())
        val response = groupRepository.createGroup(createClientId, groupName, participants, isGroup, getDomain(), getClientId(), server)
        if (response.data != null) {
            val group = groupRepository.convertGroupFromResponse(response.data, getDomain(), getClientId(), server)
            groupRepository.insertGroup(group)
            return Resource.success(group)
        }
        return Resource.error(response.message ?: "", null, response.errorCode, response.error)
    }

    private fun getClientId(): String = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain
}
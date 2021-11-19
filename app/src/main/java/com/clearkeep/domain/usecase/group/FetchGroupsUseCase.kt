package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import javax.inject.Inject

class FetchGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(): Resource<Any> {
        val servers = serverRepository.getServers()

        servers.forEach { server ->
            val fetchGroupResponse = groupRepository.fetchGroups(server.profile.userId)
            if (fetchGroupResponse.status == Status.ERROR) {
                return fetchGroupResponse
            }

            for (group in fetchGroupResponse.data ?: emptyList()) {
                val decryptedGroup = groupRepository.convertGroupFromResponse(
                    group,
                    server.serverDomain,
                    server.profile.userId
                )

                val oldGroup = groupRepository.getGroupByID(group.groupId, server.serverDomain)
                if (oldGroup?.isDeletedUserPeer != true) {
                    groupRepository.insertGroup(decryptedGroup)
                }
            }
        }

        return Resource.success(null)
    }
}
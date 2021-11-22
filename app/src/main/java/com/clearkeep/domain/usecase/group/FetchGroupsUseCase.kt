package com.clearkeep.domain.usecase.group

import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class FetchGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val environment: Environment
) {
    suspend operator fun invoke(): Resource<Any> {
        val servers = serverRepository.getServers()

        servers.forEach { server ->
            val svr = serverRepository.getServer(getDomain(), server.profile.userId)
            if (svr == null) {
                printlnCK("getGroupByID: null server")
                throw NullPointerException("fetchGroup null server")
            }
            val fetchGroupResponse = groupRepository.fetchGroups(svr)
            if (fetchGroupResponse.status == Status.ERROR) {
                return fetchGroupResponse
            }

            for (group in fetchGroupResponse.data ?: emptyList()) {
                val decryptedGroup = groupRepository.convertGroupFromResponse(
                    group,
                    server.serverDomain,
                    server.profile.userId,
                    server
                )

                val oldGroup = groupRepository.getGroupByID(group.groupId, server.serverDomain)
                if (oldGroup?.isDeletedUserPeer != true) {
                    groupRepository.insertGroup(decryptedGroup)
                }
            }
        }

        return Resource.success(null)
    }

    private fun getDomain() = environment.getServer().serverDomain
}
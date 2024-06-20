package com.clearkeep.domain.usecase.group

import android.util.Log
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.repository.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val environment: Environment
) {
    suspend operator fun invoke(): Resource<Any> = withContext(Dispatchers.IO) {
        val servers = serverRepository.getServers()

        servers.forEach { server ->
            printlnCK("FetchGroupsUseCase invoke line = 22: ${getDomain()} " );
            val svr = serverRepository.getServer(getDomain(), server.profile.userId)
            if (svr == null) {
                printlnCK("null server ${server.serverDomain}")
               // throw NullPointerException("fetchGroup null server")
            }
            else {
                val fetchGroupResponse = groupRepository.fetchGroups(svr)
                if (fetchGroupResponse.status == Status.ERROR) {
                    return@withContext fetchGroupResponse
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
        }

        return@withContext Resource.success(null)
    }

    private fun getDomain() = environment.getServer().serverDomain
}
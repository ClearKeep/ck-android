package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class FetchGroupsUseCase @Inject constructor(private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke() {
        val servers = serverRepository.getServers()

        groupRepository.fetchGroups(servers)
    }
}
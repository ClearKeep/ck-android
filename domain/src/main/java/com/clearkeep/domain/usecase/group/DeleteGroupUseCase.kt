package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long, domain: String, ownerClientId: String) = withContext(
        Dispatchers.IO) {
        groupRepository.deleteGroup(groupId, domain, ownerClientId)
    }

    suspend operator fun invoke(domain: String, ownerClientId: String) = withContext(Dispatchers.IO) {
        groupRepository.deleteGroup(domain, ownerClientId)
    }
}
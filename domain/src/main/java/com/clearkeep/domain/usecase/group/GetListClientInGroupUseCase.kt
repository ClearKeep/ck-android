package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListClientInGroupUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long, domain: String) = withContext(Dispatchers.IO) {
        groupRepository.getListClientInGroup(groupId, domain)
    }
}
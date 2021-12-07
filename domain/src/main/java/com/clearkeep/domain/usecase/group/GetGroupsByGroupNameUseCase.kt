package com.clearkeep.domain.usecase.group

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupsByGroupNameUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    operator fun invoke(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>> = groupRepository.getGroupsByGroupName(ownerDomain, ownerClientId, query)
}
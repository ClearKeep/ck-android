package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetGroupPeerByClientIdUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(friend: User, owner: Owner) = withContext(Dispatchers.IO) { groupRepository.getGroupPeerByClientId(friend, owner) }
}
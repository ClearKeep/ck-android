package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupPeerByClientIdUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(friend: com.clearkeep.domain.model.User, owner: com.clearkeep.domain.model.Owner) = groupRepository.getGroupPeerByClientId(friend, owner)
}
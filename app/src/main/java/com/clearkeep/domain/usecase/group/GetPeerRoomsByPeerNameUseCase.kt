package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetPeerRoomsByPeerNameUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    operator fun invoke(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ) = groupRepository.getPeerRoomsByPeerName(ownerDomain, ownerClientId, query)
}
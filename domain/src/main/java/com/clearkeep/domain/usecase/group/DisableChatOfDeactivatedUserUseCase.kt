package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class DisableChatOfDeactivatedUserUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(clientId: String, domain: String, userId: String) {
        val peerRooms = groupRepository.getAllPeerGroupByDomain(
            com.clearkeep.domain.model.Owner(
                domain,
                clientId
            )
        ).filter {
            printlnCK("disableChatOfDeactivatedUser peerRooms before filter $it")
            it.clientList.find { it.userId == userId } != null
        }.map { it.generateId ?: 0 }

        groupRepository.setDeletedUserPeerGroup(peerRooms)
    }
}
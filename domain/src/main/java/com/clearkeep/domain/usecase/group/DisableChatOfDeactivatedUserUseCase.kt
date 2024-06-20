package com.clearkeep.domain.usecase.group

import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DisableChatOfDeactivatedUserUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(clientId: String, domain: String, userId: String) = withContext(
        Dispatchers.IO) {
        val peerRooms = groupRepository.getAllPeerGroupByDomain(
            Owner(
                domain,
                clientId
            )
        ).filter {
            printlnCK("disableChatOfDeactivatedUser peerRooms before filter $it")
            it.clientList.find { it.userId == userId } != null
        }.map {
            it.generateId ?: 0
        }

        peerRooms.forEach {
            printlnCK("disableChatOfDeactivatedUser peerRooms before filter id delete $it")
        }

        groupRepository.setDeletedUserPeerGroup(peerRooms)
    }
}
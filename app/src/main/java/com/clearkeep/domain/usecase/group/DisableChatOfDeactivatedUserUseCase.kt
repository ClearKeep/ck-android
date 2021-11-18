package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class DisableChatOfDeactivatedUserUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(clientId: String, domain: String, userId: String) =
        groupRepository.disableChatOfDeactivatedUser(clientId, domain, userId)
}
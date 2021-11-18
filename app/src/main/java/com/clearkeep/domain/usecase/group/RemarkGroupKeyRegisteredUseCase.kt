package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class RemarkGroupKeyRegisteredUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(groupId: Long) = groupRepository.remarkGroupKeyRegistered(groupId)
}
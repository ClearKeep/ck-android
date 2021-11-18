package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetAllRoomsUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    operator fun invoke() = groupRepository.getAllRooms()
}
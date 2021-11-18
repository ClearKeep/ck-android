package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class GetFriendUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(friendClientId: String, friendDomain: String, owner: Owner) =
        peopleRepository.getFriend(friendClientId, friendDomain, owner)

    suspend operator fun invoke(friendClientId: String) =
        peopleRepository.getFriendFromID(friendClientId)
}
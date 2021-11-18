package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class GetFriendsUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    operator fun invoke(ownerDomain: String, ownerClientId: String) = peopleRepository.getFriends(ownerDomain, ownerClientId)
}
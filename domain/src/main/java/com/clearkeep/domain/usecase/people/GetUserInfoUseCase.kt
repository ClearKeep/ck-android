package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class GetUserInfoUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(userId: String, userDomain: String) =
        peopleRepository.getUserInfo(userId, userDomain)
}
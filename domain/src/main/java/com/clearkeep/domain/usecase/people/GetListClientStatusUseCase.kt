package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class GetListClientStatusUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(list: List<com.clearkeep.domain.model.User>) = peopleRepository.getListClientStatus(list)
}
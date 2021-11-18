package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class UpdateStatusUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(status: String?) = peopleRepository.updateStatus(status)
}
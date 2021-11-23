package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class UpdateAvatarUserEntityUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(user: com.clearkeep.domain.model.User, owner: com.clearkeep.domain.model.Owner) = peopleRepository.updateAvatarUserEntity(user, owner)
}
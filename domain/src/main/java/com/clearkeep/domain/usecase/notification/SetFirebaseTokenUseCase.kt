package com.clearkeep.domain.usecase.notification

import com.clearkeep.domain.repository.UserRepository
import javax.inject.Inject

class SetFirebaseTokenUseCase @Inject constructor(private val userRepository: UserRepository) {
    operator fun invoke(token: String) = userRepository.setFirebaseToken(token)
}
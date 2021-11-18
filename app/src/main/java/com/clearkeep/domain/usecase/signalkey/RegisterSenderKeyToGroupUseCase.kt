package com.clearkeep.domain.usecase.signalkey

import com.clearkeep.domain.repository.SignalKeyRepository
import javax.inject.Inject

class RegisterSenderKeyToGroupUseCase @Inject constructor(private val signalKeyRepository: SignalKeyRepository) {
    suspend operator fun invoke(groupID: Long, clientId: String, domain: String) =
        signalKeyRepository.registerSenderKeyToGroup(groupID, clientId, domain)
}
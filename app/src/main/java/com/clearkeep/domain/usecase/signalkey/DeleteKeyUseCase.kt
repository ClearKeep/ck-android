package com.clearkeep.domain.usecase.signalkey

import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.SignalKeyRepository
import javax.inject.Inject

class DeleteKeyUseCase @Inject constructor(private val signalKeyRepository: SignalKeyRepository) {
    suspend operator fun invoke(owner: Owner, server: Server, chatGroups: List<ChatGroup>?) = signalKeyRepository.deleteKey(owner, server, chatGroups)
}
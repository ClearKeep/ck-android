package com.clearkeep.domain.repository

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.data.local.signal.model.SignalIdentityKey
import com.clearkeep.domain.model.ChatGroup

interface SignalKeyRepository {
    fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey?
    suspend fun deleteKey(owner: Owner, server: Server, chatGroups: List<ChatGroup>?)
    suspend fun registerSenderKeyToGroup(groupID: Long, clientId: String, domain: String): Boolean
}
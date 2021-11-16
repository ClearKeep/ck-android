package com.clearkeep.domain.repository

import com.clearkeep.db.clearkeep.model.ChatGroup
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.db.signalkey.model.SignalIdentityKey

interface SignalKeyRepository {
    fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey?
    suspend fun deleteKey(owner: Owner, server: Server, chatGroups: List<ChatGroup>?)
    suspend fun registerSenderKeyToGroup(groupID: Long, clientId: String, domain: String): Boolean
}
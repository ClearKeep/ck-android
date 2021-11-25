package com.clearkeep.domain.repository

import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyStore

interface SenderKeyStore: SenderKeyStore {
    fun hasSenderKey(senderKeyName: SenderKeyName): Boolean
    suspend fun deleteSenderKey(senderKeyName: SenderKeyName)
}
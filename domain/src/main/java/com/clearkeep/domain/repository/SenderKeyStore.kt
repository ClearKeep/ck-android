package com.clearkeep.domain.repository

import com.clearkeep.domain.model.CKSignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyStore


interface SenderKeyStore: SenderKeyStore {
    fun hasSenderKey(senderKeyName: CKSignalProtocolAddress): Boolean
    suspend fun deleteSenderKey(senderKeyName: CKSignalProtocolAddress)
    suspend fun getAllSenderKey()
}
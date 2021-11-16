package com.clearkeep.data.local.signal

import com.clearkeep.db.signalkey.dao.SignalKeyDAO
import com.clearkeep.db.signalkey.model.SignalSenderKey
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.groups.state.SenderKeyStore
import java.io.IOException
import java.util.*

class InMemorySenderKeyStore(
    private val signalKeyDAO: SignalKeyDAO,
) : SenderKeyStore {

    private val store: MutableMap<SenderKeyName, SenderKeyRecord> = HashMap()

    override fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) {
        store[senderKeyName] = record
        signalKeyDAO.insert(
            SignalSenderKey(
                senderKeyName.groupId + senderKeyName.sender.name,
                senderKeyName.groupId,
                senderKeyName.sender.name,
                senderKeyName.sender.deviceId,
                record.serialize()
            )
        )
    }

    override fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord {
        return try {
            var record = store[senderKeyName]

            if (record == null) {
                val senderKey = signalKeyDAO.getSignalSenderKey(
                    senderKeyName.groupId,
                    senderKeyName.sender.name,
                    senderKeyName.sender.deviceId
                )
                if (senderKey != null) {
                    record = SenderKeyRecord(senderKey.senderKey)
                    // update cache
                    store[senderKeyName] = record
                }
            }

            if (record == null) {
                SenderKeyRecord()
            } else {
                SenderKeyRecord(record.serialize())
            }
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    fun hasSenderKey(senderKeyName: SenderKeyName): Boolean {
        try {
            val senderKey = signalKeyDAO.getSignalSenderKey(
                senderKeyName.groupId,
                senderKeyName.sender.name,
                senderKeyName.sender.deviceId
            )
            if (senderKey.senderKey.isEmpty()) return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun deleteSenderKey(senderKeyName: SenderKeyName) {
        signalKeyDAO.deleteSignalSenderKey(senderKeyName.groupId, senderKeyName.sender.name)
    }
}
package com.clearkeep.data.local.signal.store

import android.util.Log
import androidx.lifecycle.LiveData
import com.clearkeep.data.local.signal.senderkey.SignalKeyDAO
import com.clearkeep.data.local.signal.senderkey.SignalSenderKey
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import java.io.IOException
import java.util.*
import com.clearkeep.domain.repository.SenderKeyStore
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySenderKeyStore @Inject constructor(
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

    override fun hasSenderKey(senderKeyName: SenderKeyName): Boolean {
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

    override suspend fun deleteSenderKey(senderKeyName: SenderKeyName) {
        val deleteSenderKey = signalKeyDAO.deleteSignalSenderKey(senderKeyName.groupId, senderKeyName.sender.name)
        Log.d("antx: ", "deleteSenderKey line = 76:$ ${senderKeyName.groupId}: $deleteSenderKey ");
    }

    override suspend fun getAllSenderKey() {
        signalKeyDAO.getSignalSenderKeys().forEach {
            Log.d("antx: ", "getAllSenderKey groupId: ${it.groupId} ${it.senderKey} ");
        }
    }
}
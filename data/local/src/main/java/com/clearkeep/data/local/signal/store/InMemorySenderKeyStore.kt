package com.clearkeep.data.local.signal.store

import android.util.Log
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.signal.senderkey.SignalKeyDAO
import com.clearkeep.data.local.signal.senderkey.SignalSenderKey
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.repository.SenderKeyStore
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.util.Pair
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySenderKeyStore @Inject constructor(
    private val signalKeyDAO: SignalKeyDAO,
) : SenderKeyStore {

    private val store: Map<Pair<SignalProtocolAddress, UUID>, SenderKeyRecord> = HashMap()

    override fun hasSenderKey(senderKeyName: CKSignalProtocolAddress): Boolean {
        try {
            val senderKey = signalKeyDAO.getSignalSenderKey(
                senderKeyName.name,
                senderKeyName.deviceId
            )
            if (senderKey?.senderKey?.isEmpty() != true) return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override suspend fun deleteSenderKey(senderKeyName: CKSignalProtocolAddress) {
        printlnCK("deleteSenderKey name: ${senderKeyName.name}")
        signalKeyDAO.deleteSignalSenderKey(senderKeyName.name)
    }

    override suspend fun getAllSenderKey() {
        signalKeyDAO.getSignalSenderKeys()
    }

    override fun storeSenderKey(sender: SignalProtocolAddress?, distributionId: UUID?, record: SenderKeyRecord?) {
        signalKeyDAO.insert(
            SignalSenderKey(
                id = distributionId.toString() + sender?.name, groupId = "groupId", senderName = sender?.name ?: "",
                deviceId = sender?.deviceId ?: -1,
                senderKey = record?.serialize() ?: "".toByteArray()
            )
        )
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID?): SenderKeyRecord? {
        return try {
            Log.d("CKL_: ", "InMemorySenderKeyStore loadSenderKey line = 63:sender name: ${sender.name} distributionId: $distributionId" );
            var record = store[Pair<SignalProtocolAddress, UUID>(sender, distributionId)]
            if (record == null) {
                val senderKey = signalKeyDAO.getSignalSenderKey(
                    sender.name,
                    sender.deviceId
                )
                if (senderKey != null) {
                    record = SenderKeyRecord(senderKey.senderKey)
                    // update cache
                } else {
                    printlnCK("loadSenderKey:senderKey record null ")
                    return null
                }
            }
            SenderKeyRecord(record.serialize())

        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }
}
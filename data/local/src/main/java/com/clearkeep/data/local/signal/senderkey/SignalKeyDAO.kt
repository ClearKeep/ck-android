package com.clearkeep.data.local.signal.senderkey

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord

@Dao
interface SignalKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(senderKey: SignalSenderKey)

    @Query("SELECT * FROM signalsenderkey")
    suspend fun getSignalSenderKeys(): List<SignalSenderKey>

    @Query("SELECT * FROM signalsenderkey WHERE sender_name = :senderName AND device_id = :deviceId LIMIT 1")
    fun getSignalSenderKey( senderName: String, deviceId: Int): SignalSenderKey?

    @Query("DELETE FROM signalsenderkey WHERE sender_name = :senderName")
    suspend fun deleteSignalSenderKey(senderName: String): Int
}
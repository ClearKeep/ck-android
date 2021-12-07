package com.clearkeep.data.local.signal.senderkey

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface SignalKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(senderKey: SignalSenderKey)

    @Query("SELECT * FROM signalsenderkey")
    fun getSignalSenderKeys(): LiveData<List<SignalSenderKey>>

    @Query("SELECT * FROM signalsenderkey WHERE group_id = :groupId AND sender_name = :senderName AND device_id = :deviceId LIMIT 1")
    fun getSignalSenderKey(groupId: String, senderName: String, deviceId: Int): SignalSenderKey

    @Query("DELETE FROM signalsenderkey WHERE group_id =:groupId AND sender_name = :senderName")
    suspend fun deleteSignalSenderKey(groupId: String, senderName: String): Int
}
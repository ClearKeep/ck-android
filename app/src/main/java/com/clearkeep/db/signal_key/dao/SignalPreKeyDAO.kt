package com.clearkeep.db.signal_key.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.signal_key.model.SignalPreKey

@Dao
interface SignalPreKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(preKey: SignalPreKey)

    @Query("SELECT * FROM signalprekey WHERE preKeyId =:preKeyId AND is_signed_key = 0 LIMIT 1")
    fun getUnSignedPreKey(preKeyId: Int): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 0")
    fun getAllUnSignedPreKey(): List<SignalPreKey>

    @Query("SELECT * FROM signalprekey WHERE preKeyId =:preKeyId AND is_signed_key = 1 LIMIT 1")
    fun getSignedPreKey(preKeyId: Int): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 1")
    fun getAllSignedPreKey(): List<SignalPreKey>
}
package com.clearkeep.db.signal_key.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.signal_key.model.SignalIdentityKey

@Dao
interface SignalIdentityKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(signalIdentityKey: SignalIdentityKey)

    @Query("SELECT * FROM signalidentitykey LIMIT 1")
    fun getIdentityKey(): SignalIdentityKey?
}
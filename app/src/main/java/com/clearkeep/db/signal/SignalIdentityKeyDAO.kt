package com.clearkeep.db.signal

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.signal.model.SignalIdentityKey

@Dao
interface SignalIdentityKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(signalIdentityKey: SignalIdentityKey)

    @Query("SELECT * FROM signalidentitykey LIMIT 1")
    fun getIdentityKey(): SignalIdentityKey?
}
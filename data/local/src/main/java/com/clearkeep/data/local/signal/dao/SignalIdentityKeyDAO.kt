package com.clearkeep.data.local.signal.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.data.local.signal.model.SignalIdentityKeyLocal

@Dao
interface SignalIdentityKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(signalIdentityKey: SignalIdentityKeyLocal)

    @Query("SELECT * FROM signalidentitykey WHERE user_id = :clientId AND domain = :domain LIMIT 1")
    fun getIdentityKey(clientId: String, domain: String): SignalIdentityKeyLocal?

    @Query("SELECT * FROM signalidentitykey")
    fun getAllIdentityKey(): List<SignalIdentityKeyLocal>

    @Query("DELETE  FROM signalidentitykey WHERE  user_id = :clientId AND domain = :domain")
    suspend fun deleteSignalKeyByOwnerDomain(clientId: String, domain: String): Int
}
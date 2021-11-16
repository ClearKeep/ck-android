package com.clearkeep.db.signalkey.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.signalkey.model.SignalIdentityKey

@Dao
interface SignalIdentityKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(signalIdentityKey: SignalIdentityKey)

    @Query("SELECT * FROM signalidentitykey WHERE user_id = :clientId AND domain = :domain LIMIT 1")
    fun getIdentityKey(clientId: String, domain: String): SignalIdentityKey?

    @Query("SELECT * FROM signalidentitykey")
    fun getAllIdentityKey(): List<SignalIdentityKey>

    @Query("DELETE  FROM signalidentitykey WHERE  user_id = :clientId AND domain = :domain")
    suspend fun deleteSignalKeyByOwnerDomain(clientId: String, domain: String): Int
}
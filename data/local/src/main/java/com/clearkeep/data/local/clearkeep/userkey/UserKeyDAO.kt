package com.clearkeep.data.local.clearkeep.userkey

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserKeyDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userKey: UserKeyEntity)

    @Query("SELECT * FROM userkey WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun getKey(serverDomain: String, userId: String): UserKeyEntity?
}
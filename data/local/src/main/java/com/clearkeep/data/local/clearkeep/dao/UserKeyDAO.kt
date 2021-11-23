package com.clearkeep.data.local.clearkeep.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearkeep.domain.model.UserKey

@Dao
interface UserKeyDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userKey: UserKey)

    @Query("SELECT * FROM userkey WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun getKey(serverDomain: String, userId: String): UserKey?
}
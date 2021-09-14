package com.clearkeep.db.clear_keep.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearkeep.db.clear_keep.model.UserKey
import com.clearkeep.db.clear_keep.model.UserPreference

@Dao
interface UserKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userKey: UserKey)

    @Query("SELECT * FROM userkey WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun getUserKey(serverDomain: String, userId: String): UserKey?

    @Query("UPDATE userkey SET k = :key WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateKey(serverDomain: String, userId: String, key: String)

    @Query("UPDATE userkey SET salt = :salt WHERE server_domain = :serverDomain AND user_id = :userId")
    suspend fun updateSalt(serverDomain: String, userId: String, salt: String)
}
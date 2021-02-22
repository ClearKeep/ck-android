package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.clear_keep.model.User

@Dao
interface UserDao {
    @Insert(onConflict = REPLACE)
    suspend fun save(user: User)

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): User?
}
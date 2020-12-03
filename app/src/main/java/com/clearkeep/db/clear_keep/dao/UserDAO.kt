package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.clear_keep.model.User

@Dao
interface UserDao {
    @Insert(onConflict = REPLACE)
    suspend fun save(user: User)

    @Query("SELECT * FROM user WHERE user_name =:userName LIMIT 1")
    suspend fun getUserByName(userName: String): User?

    @Query("SELECT * FROM user")
    fun getUser(): LiveData<User>
}
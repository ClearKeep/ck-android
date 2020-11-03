package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.User

@Dao
interface UserDao {
    @Insert(onConflict = REPLACE)
    fun save(user: User)

    @Query("SELECT * FROM user")
    fun getUser(): LiveData<User>
}
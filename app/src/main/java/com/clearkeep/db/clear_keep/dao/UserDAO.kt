package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.clearkeep.db.clear_keep.model.User

@Dao
interface UserDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(people: User)

    @Insert(onConflict = REPLACE)
    suspend fun insertPeopleList(people: List<User>)

    @Query("SELECT * FROM user WHERE user_id =:userId AND owner_domain = :domain LIMIT 1")
    suspend fun getFriend(userId: String, domain: String): User?

    @Query("SELECT * FROM user WHERE owner_domain = :ownerDomain")
    fun getFriends(ownerDomain: String): LiveData<List<User>>
}
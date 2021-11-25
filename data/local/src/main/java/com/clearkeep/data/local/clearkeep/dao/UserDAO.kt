package com.clearkeep.data.local.clearkeep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.clearkeep.data.local.model.UserEntityLocal
import com.clearkeep.domain.model.UserEntity

@Dao
interface UserDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(people: UserEntityLocal)

    @Insert(onConflict = REPLACE)
    suspend fun insertPeopleList(people: List<UserEntityLocal>)

    @Query("SELECT * FROM userentity WHERE user_id =:userId AND domain = :domain AND owner_domain = :ownerDomain AND owner_client_id = :ownerClientId LIMIT 1")
    suspend fun getFriend(
        userId: String,
        domain: String,
        ownerDomain: String,
        ownerClientId: String
    ): UserEntityLocal?

    @Query("SELECT * FROM userentity WHERE user_id =:userId LIMIT 1")
    suspend fun getFriendFromUserId(userId: String): UserEntityLocal?

    @Query("SELECT * FROM userentity WHERE owner_domain = :ownerDomain AND owner_client_id = :ownerClientId")
    fun getFriends(ownerDomain: String, ownerClientId: String): LiveData<List<UserEntityLocal>>

    @Query("DELETE  FROM userentity WHERE  user_id = :ownerId")
    suspend fun deleteFriend(ownerId: String): Int
}
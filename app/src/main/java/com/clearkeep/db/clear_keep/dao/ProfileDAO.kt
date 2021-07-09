package com.clearkeep.db.clear_keep.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.clear_keep.model.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = REPLACE)
    suspend fun save(profile: Profile)

    @Query("SELECT * FROM profile WHERE user_id = :id LIMIT 1")
    suspend fun getProfile(id: String): Profile
}
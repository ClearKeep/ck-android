package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.clearkeep.db.model.People

@Dao
interface PeopleDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(people: People)

    @Insert(onConflict = REPLACE)
    suspend fun insertPeopleList(people: List<People>)

    @Query("SELECT * FROM people")
    fun getPeople(): LiveData<List<People>>
}
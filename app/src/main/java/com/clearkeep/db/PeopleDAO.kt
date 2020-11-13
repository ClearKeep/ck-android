package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.People

@Dao
interface PeopleDao {
    @Insert(onConflict = REPLACE)
    fun insert(people: People)

    @Query("SELECT * FROM people")
    fun getPeople(): LiveData<List<People>>
}
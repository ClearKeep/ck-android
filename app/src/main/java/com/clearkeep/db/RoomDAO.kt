package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.clearkeep.db.model.Room

@Dao
interface RoomDAO {
    @Insert(onConflict = REPLACE)
    fun insert(room: Room)

    @Query("SELECT * FROM room WHERE is_group = 0")
    fun getSingleRooms(): LiveData<List<Room>>
}
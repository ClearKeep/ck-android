package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.clearkeep.db.model.Room

@Dao
interface RoomDAO {
    @Insert(onConflict = REPLACE)
    fun insert(room: Room)

    @Update
    fun update(vararg room: Room)

    @Query("SELECT * FROM room WHERE is_group = 0")
    fun getPeerRooms(): LiveData<List<Room>>

    @Query("SELECT * FROM room WHERE is_group = 1")
    fun getGroupRooms(): LiveData<List<Room>>

    @Query("SELECT id FROM room WHERE remote_id = :remoteId LIMIT 1")
    fun getRoomId(remoteId: String): Int?

    @Query("SELECT * FROM room WHERE id = :roomId LIMIT 1")
    fun getRoomFromIdAsState(roomId: Int): LiveData<Room>

    @Query("SELECT * FROM room WHERE id = :roomId LIMIT 1")
    fun getRoomFromId(roomId: Int): Room
}
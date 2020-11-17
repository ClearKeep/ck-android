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
    suspend fun insert(room: Room) : Long

    @Update
    suspend fun update(vararg room: Room)

    @Query("SELECT * FROM room WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getRoomByRemoteId(remoteId: String): Room

    @Query("SELECT * FROM room WHERE id = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Int): Room

    // tracking
    @Query("SELECT * FROM room")
    fun getRooms(): LiveData<List<Room>>
}
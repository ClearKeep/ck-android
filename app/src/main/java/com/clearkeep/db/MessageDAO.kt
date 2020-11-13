package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.Message

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    fun insert(message: Message)

    @Query("SELECT * FROM message WHERE room_id = :roomId ORDER BY created_time ASC")
    fun getMessages(roomId: Int): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE sender_id = :remoteId OR receiver_id = :remoteId ORDER BY created_time ASC")
    fun getMessagesFromAFriend(remoteId: String): LiveData<List<Message>>
}
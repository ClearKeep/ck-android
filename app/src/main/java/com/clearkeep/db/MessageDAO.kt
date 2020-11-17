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

    @Query("SELECT * FROM message WHERE sender_id = :senderId OR receiver_id = :receiverId")
    fun getMessagesFromAFriend(receiverId: String, senderId: String): LiveData<List<Message>>
}
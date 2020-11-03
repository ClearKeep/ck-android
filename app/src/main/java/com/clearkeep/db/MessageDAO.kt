package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.User

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    fun insert(message: Message)

    @Query("SELECT * FROM message WHERE sender_id = :senderId")
    fun getMessages(senderId: String): LiveData<List<Message>>
}
package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.Message

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    fun insert(message: Message)

    @Query("SELECT * FROM message WHERE group_id = :groupId ORDER BY created_time ASC")
    fun getMessages(groupId: String): LiveData<List<Message>>
}
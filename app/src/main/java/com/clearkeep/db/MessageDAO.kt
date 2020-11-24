package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.Message

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(message: Message)

    @Insert(onConflict = REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("DELETE FROM message WHERE group_id =:groupId")
    suspend fun deleteMessageFromGroupId(groupId: String)

    @Query("SELECT * FROM message WHERE group_id = :groupId ORDER BY created_time ASC")
    fun getMessages(groupId: String): LiveData<List<Message>>
}
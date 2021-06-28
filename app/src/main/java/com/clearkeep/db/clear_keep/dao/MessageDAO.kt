package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.clear_keep.model.Message

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(message: Message)

    @Insert(onConflict = REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("DELETE FROM message WHERE group_id =:groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId")
    suspend fun deleteMessageFromGroupId(groupId: Long, domain: String, ownerClientId: String)

    @Query("SELECT * FROM message WHERE id = :messageId")
    suspend fun getMessage(messageId: String): Message?

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId ORDER BY created_time ASC")
    suspend fun getMessages(groupId: Long, domain: String, ownerClientId: String): List<Message>

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId AND created_time > :fromTime")
    suspend fun getMessagesAfterTime(groupId: Long, fromTime: Long, domain: String, ownerClientId: String): List<Message>

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId ORDER BY created_time ASC")
    fun getMessagesAsState(groupId: Long, domain: String, ownerClientId: String): LiveData<List<Message>>
}
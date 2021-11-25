package com.clearkeep.data.local.clearkeep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.data.local.model.MessageLocal
import com.clearkeep.domain.model.Message

@Dao
interface MessageDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(message: MessageLocal): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertMessages(messages: List<MessageLocal>)

    @Update
    suspend fun updateMessage(message: MessageLocal)

    @Query("DELETE FROM message WHERE group_id =:groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId")
    suspend fun deleteMessageFromGroupId(groupId: Long, domain: String, ownerClientId: String)

    @Query("SELECT * FROM message WHERE message_id = :messageId AND group_id =:groupId")
    suspend fun getMessage(messageId: String, groupId: Long): MessageLocal?

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId ORDER BY created_time ASC")
    suspend fun getMessages(groupId: Long, domain: String, ownerClientId: String): List<MessageLocal>

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId AND created_time > :fromTime")
    suspend fun getMessagesAfterTime(
        groupId: Long,
        fromTime: Long,
        domain: String,
        ownerClientId: String
    ): List<MessageLocal>

    @Query("SELECT * FROM message WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerClientId GROUP BY message_id ORDER BY created_time DESC")
    fun getMessagesAsState(
        groupId: Long,
        domain: String,
        ownerClientId: String
    ): LiveData<List<MessageLocal>>

    @Query("DELETE FROM message WHERE message_id = ''")
    suspend fun deleteTempMessages()

    @Query("SELECT * FROM message WHERE owner_domain = :ownerDomain AND owner_client_id = :ownerClientId AND message LIKE :query GROUP BY message_id")
    fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<MessageLocal>>

    @Query("DELETE FROM message WHERE owner_domain = :domain AND owner_client_id = :ownerClientId")
    suspend fun deleteMessageByDomain(domain: String, ownerClientId: String)
}
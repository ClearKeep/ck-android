package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.clearkeep.db.clear_keep.model.ChatGroup

@Dao
interface GroupDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(group: ChatGroup) : Long

    @Insert(onConflict = REPLACE)
    suspend fun insertGroupList(groups: List<ChatGroup>)

    @Update
    suspend fun update(vararg group: ChatGroup)

    @Query("SELECT * FROM chatgroup WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): ChatGroup

    @Query("SELECT * FROM chatgroup WHERE group_type = \"peer\"")
    suspend fun getPeerGroups(): List<ChatGroup>

    @Query("SELECT * FROM chatgroup")
    suspend fun getRooms(): List<ChatGroup>

    // tracking
    @Query("SELECT * FROM chatgroup")
    fun getRoomsAsState(): LiveData<List<ChatGroup>>

/*    @Query("SELECT chatgroup.*, message.* FROM chatgroup LEFT JOIN message ON chatgroup.last_message_id = message.id")
    fun getRoomWithLastMessageAsState(): LiveData<List<GroupAndLastMessage>>*/
}
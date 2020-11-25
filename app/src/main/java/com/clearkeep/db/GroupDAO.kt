package com.clearkeep.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.db.model.ChatGroup

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

    @Query("SELECT * FROM chatgroup WHERE group_type =:groupType AND lst_client_id =:clientIdOfString LIMIT 1")
    suspend fun getGroupPeerByClientId(clientIdOfString: String, groupType:String = "peer"): ChatGroup

    // tracking
    @Query("SELECT * FROM chatgroup")
    fun getRooms(): LiveData<List<ChatGroup>>
}
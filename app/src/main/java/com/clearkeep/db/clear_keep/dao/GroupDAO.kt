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
    suspend fun updateGroup(vararg group: ChatGroup)

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String, ownerId: String): ChatGroup?

    @Query("SELECT * FROM chatgroup WHERE group_type = \"peer\" AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun getPeerGroups(domain: String, ownerId: String): List<ChatGroup>

    @Query("SELECT * FROM chatgroup")
    suspend fun getRooms(): List<ChatGroup>

    // tracking
    @Query("SELECT * FROM chatgroup ORDER BY updated_at DESC")
    fun getRoomsAsState(): LiveData<List<ChatGroup>>

    @Query("DELETE  FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupById(groupId: Long, domain: String, ownerId: String): Int

    @Query("UPDATE chatgroup SET is_deleted_user_peer = 1 WHERE generateId IN (:ids)")
    suspend fun disableChatOfDeactivatedUser(ids: List<Int>)
/*    @Query("SELECT chatgroup.*, message.* FROM chatgroup LEFT JOIN message ON chatgroup.last_message_id = message.id")
    fun getRoomWithLastMessageAsState(): LiveData<List<GroupAndLastMessage>>*/

    @Query("DELETE  FROM chatgroup WHERE  owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupByOwnerDomain( domain: String, ownerId: String): Int

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" AND group_name LIKE :query")
    fun getGroupsByGroupName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"peer\" AND group_name LIKE :query")
    fun getPeerRoomsByPeerName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" ")
    fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String): ChatGroup?
}
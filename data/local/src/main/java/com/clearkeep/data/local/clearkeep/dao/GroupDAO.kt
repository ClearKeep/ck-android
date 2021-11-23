package com.clearkeep.data.local.clearkeep.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.clearkeep.domain.model.ChatGroup

@Dao
interface GroupDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(group: ChatGroup): Long

    @Update
    suspend fun updateGroup(vararg group: ChatGroup)

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String, ownerId: String): ChatGroup?

    @Query("SELECT * FROM chatgroup WHERE group_type = \"peer\" AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun getPeerGroups(domain: String, ownerId: String): List<ChatGroup>

    @Query("SELECT * FROM chatgroup ORDER BY updated_at DESC")
    fun getRoomsAsState(): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup ORDER BY updated_at DESC")
    fun getRooms(): List<ChatGroup>

    @Query("DELETE  FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupById(groupId: Long, domain: String, ownerId: String): Int

    @Query("UPDATE chatgroup SET is_deleted_user_peer = 1 WHERE generateId IN (:ids)")
    suspend fun setDeletedUserPeerGroup(ids: List<Int>)

    @Query("DELETE  FROM chatgroup WHERE  owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupByOwnerDomain(domain: String, ownerId: String): Int

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" AND group_name LIKE :query")
    fun getGroupsByGroupName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"peer\" AND group_name LIKE :query")
    fun getPeerRoomsByPeerName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" ")
    fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroup>>

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String): ChatGroup?
}
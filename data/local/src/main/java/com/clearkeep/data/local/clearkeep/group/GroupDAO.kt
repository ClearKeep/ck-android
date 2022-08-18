package com.clearkeep.data.local.clearkeep.group

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update

@Dao
interface GroupDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(group: ChatGroupEntity): Long

    @Update
    suspend fun updateGroup(vararg group: ChatGroupEntity)

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String, ownerId: String): ChatGroupEntity?

    @Query("SELECT * FROM chatgroup WHERE group_type = \"peer\" AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun getPeerGroups(domain: String, ownerId: String): List<ChatGroupEntity>

    @Query("SELECT * FROM chatgroup ORDER BY updated_at DESC")
    fun getRoomsAsState(): LiveData<List<ChatGroupEntity>>

    @Query("SELECT * FROM chatgroup ORDER BY updated_at DESC")
    fun getRooms(): List<ChatGroupEntity>

    @Query("DELETE  FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupById(groupId: Long, domain: String, ownerId: String): Int

    @Query("UPDATE chatgroup SET group_name = 'Deleted User' WHERE generateId IN (:ids)")
    suspend fun setDeletedUserPeerGroup(ids: List<Int>)

    @Query("DELETE  FROM chatgroup WHERE  owner_domain = :domain AND owner_client_id = :ownerId")
    suspend fun deleteGroupByOwnerDomain(domain: String, ownerId: String): Int

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" AND group_name LIKE :query")
    fun getGroupsByGroupName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroupEntity>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"peer\" AND group_name LIKE :query")
    fun getPeerRoomsByPeerName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroupEntity>>

    @Query("SELECT * FROM chatgroup WHERE owner_domain=:ownerDomain AND owner_client_id=:ownerClientId AND group_type = \"group\" ")
    fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroupEntity>>

    @Query("SELECT * FROM chatgroup WHERE group_id = :groupId AND owner_domain = :domain LIMIT 1")
    suspend fun getGroupById(groupId: Long, domain: String): ChatGroupEntity?
}
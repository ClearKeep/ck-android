package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GroupObjectResponse

interface GroupRepository {
    fun getAllRoomsAsState(): LiveData<List<ChatGroup>>
    suspend fun getAllRooms(): List<ChatGroup>
    suspend fun setDeletedUserPeerGroup(peerRoomsId: List<Int>)
    suspend fun createGroup(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean,
        domain: String,
        clientId: String,
        server: Server?
    ): Resource<GroupObjectResponse>

    suspend fun updateGroup(group: ChatGroup)
    suspend fun removeMemberInGroup(removedUser: User, groupId: Long, owner: Owner): Boolean
    suspend fun leaveGroup(groupId: Long, owner: Owner): String?
    suspend fun getGroupByGroupId(groupId: Long): ChatGroup?
    suspend fun fetchGroups(server: Server): Resource<List<GroupObjectResponse>>
    suspend fun convertGroupFromResponse(
        response: GroupObjectResponse,
        serverDomain: String,
        ownerId: String,
        server: Server?
    ): ChatGroup
    suspend fun insertGroup(group: ChatGroup)
    suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String, server: Server?, forceRefresh: Boolean): Resource<ChatGroup>
    suspend fun getGroupByID(groupId: Long, serverDomain: String): ChatGroup?
    suspend fun deleteGroup(groupId: Long, domain: String, ownerClientId: String)
    suspend fun deleteGroup(domain: String, ownerClientId: String)
    suspend fun getGroupPeerByClientId(friend: User, owner: Owner): ChatGroup?
    fun getGroupsByGroupName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>>

    fun getPeerRoomsByPeerName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>>

    fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroup>>
    suspend fun getAllPeerGroupByDomain(owner: Owner): List<ChatGroup>
    suspend fun getListClientInGroup(
        groupId: Long,
        domain: String
    ): List<String>?
    suspend fun inviteUserToGroup(
        invitedUsers: List<User>,
        groupId: Long,
        owner: Owner
    ): String?
    suspend fun getGroup(
        groupId: Long,
        owner: Owner,
        server: Server?
    ): Resource<ChatGroup>
}
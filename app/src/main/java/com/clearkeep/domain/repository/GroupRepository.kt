package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.User
import com.clearkeep.utilities.network.Resource
import group.GroupOuterClass

interface GroupRepository {
    fun getAllRoomsAsState(): LiveData<List<ChatGroup>>
    suspend fun getAllRooms(): List<ChatGroup>
    suspend fun disableChatOfDeactivatedUser(clientId: String, domain: String, userId: String)
    suspend fun createGroup(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean
    ): Resource<ChatGroup>?

    suspend fun inviteToGroup(
        invitedUsers: List<User>,
        groupId: Long,
        server: Server?,
        owner: Owner
    ): Resource<ChatGroup>

    suspend fun removeMemberInGroup(removedUser: User, groupId: Long, owner: Owner): Boolean
    suspend fun leaveGroup(groupId: Long, owner: Owner): Boolean
    suspend fun getGroupByGroupId(groupId: Long): ChatGroup?
    suspend fun fetchGroups(clientId: String): Resource<List<GroupOuterClass.GroupObjectResponse>>
    suspend fun convertGroupFromResponse(
        response: GroupOuterClass.GroupObjectResponse,
        serverDomain: String,
        ownerId: String
    ): ChatGroup
    suspend fun insertGroup(group: ChatGroup)
    suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String): Resource<ChatGroup>
    suspend fun getGroupByID(groupId: Long, serverDomain: String): ChatGroup?
    suspend fun deleteGroup(groupId: Long, domain: String, ownerClientId: String)
    suspend fun deleteGroup(domain: String, ownerClientId: String)
    suspend fun getGroupPeerByClientId(friend: User, owner: Owner): ChatGroup?
    suspend fun remarkGroupKeyRegistered(groupId: Long): ChatGroup
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
}
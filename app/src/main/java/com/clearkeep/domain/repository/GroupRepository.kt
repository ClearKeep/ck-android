package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.db.clearkeep.model.ChatGroup
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.clearkeep.model.User
import com.clearkeep.utilities.network.Resource

interface GroupRepository {
    fun getAllRooms(): LiveData<List<ChatGroup>>
    fun getClientId(): String
    suspend fun fetchGroups()
    suspend fun disableChatOfDeactivatedUser(clientId: String, domain: String, userId: String)
    suspend fun createGroup(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean
    ): Resource<ChatGroup>?
    suspend fun inviteToGroupFromAPIs(
        invitedUsers: List<User>,
        groupId: Long,
        owner: Owner
    ): Resource<ChatGroup>
    suspend fun removeMemberInGroup(removedUser: User, groupId: Long, owner: Owner): Boolean
    suspend fun leaveGroup(groupId: Long, owner: Owner): Boolean
    suspend fun getGroupByGroupId(groupId: Long): ChatGroup?
    fun getTemporaryGroupWithAFriend(createPeople: User, receiverPeople: User): ChatGroup
    suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String): Resource<ChatGroup>?
    suspend fun getGroupFromAPIById(
        groupId: Long,
        domain: String,
        ownerId: String
    ): Resource<ChatGroup>?
    suspend fun removeGroupOnWorkSpace(groupId: Long, domain: String, ownerClientId: String)
    suspend fun removeGroupByDomain(domain: String, ownerClientId: String)
    suspend fun getGroupPeerByClientId(friend: User, owner: Owner): ChatGroup?
    suspend fun remarkGroupKeyRegistered(groupId: Long): ChatGroup
    fun getGroupsByGroupName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>>
    fun getPeerRoomsByPeerName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>>
    fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroup>>
    suspend fun getAllPeerGroupByDomain(owner: Owner): List<ChatGroup>
    suspend fun getListClientInGroup(
        groupId: Long,
        domain: String
    ): List<String>?
}
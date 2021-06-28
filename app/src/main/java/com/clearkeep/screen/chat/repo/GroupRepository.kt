package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.printlnCK
import group.GroupGrpc
import group.GroupOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    // dao
    private val groupDAO: GroupDAO,

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    private val environment: Environment
) {
    fun getAllRooms() = groupDAO.getRoomsAsState()

    fun getClientId() = environment.getServer().profile.id

    private fun getDomain() = environment.getServer().serverDomain

    @Throws(Exception::class)
    suspend fun getRoomsFromAPI(groupGrpc: GroupGrpc.GroupBlockingStub, clientId: String) : List<GroupOuterClass.GroupObjectResponse>  = withContext(Dispatchers.IO) {
        printlnCK("getRoomsFromAPI: $clientId")
        val request = GroupOuterClass.GetJoinedGroupsRequest.newBuilder()
                .setClientId(clientId)
                .build()
        val response = groupGrpc.getJoinedGroups(request)
        printlnCK("getRoomsFromAPI, ${response.lstGroupList}")
        return@withContext response.lstGroupList
    }

    suspend fun createGroupFromAPI(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean
    ): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("createGroup: $groupName, clients $participants")
        try {
            val clients = participants.map { people ->
                GroupOuterClass.ClientInGroupObject.newBuilder()
                    .setId(people.id)
                    .setDisplayName(people.userName)
                    .setWorkspaceDomain(people.ownerDomain).build()
            }
            val request = GroupOuterClass.CreateGroupRequest.newBuilder()
                .setGroupName(groupName)
                .setCreatedByClientId(createClientId)
                .addAllLstClient(clients)
                .setGroupType(getGroupType(isGroup))
                .build()
            val response = dynamicAPIProvider.provideGroupBlockingStub().createGroup(request)

            val group = convertGroupFromResponse(response, dynamicAPIProvider.provideSignalKeyDistributionBlockingStub(), getDomain(), getClientId())

            // save to database
            insertGroup(group)
            printlnCK("createGroup success, $group")
            return@withContext group
        } catch (e: Exception) {
            printlnCK("createGroup error: $e")
            return@withContext null
        }
    }

    suspend fun inviteToGroupFromAPI(ourClientId: String, invitedFriendId: String, groupId: Long): Boolean = withContext(Dispatchers.IO) {
        printlnCK("inviteToGroup: $invitedFriendId")
        try {
            val request = GroupOuterClass.InviteToGroupRequest.newBuilder()
                    .setFromClientId(ourClientId)
                    .setClientId(invitedFriendId)
                    .setGroupId(groupId)
                    .build()
            val response = dynamicAPIProvider.provideGroupBlockingStub().inviteToGroup(request)

            return@withContext response.success
        } catch (e: Exception) {
            printlnCK("inviteToGroup error: $e")
            return@withContext false
        }
    }

    private suspend fun getGroupFromAPI(groupId: Long, signalGrpc: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("getGroupFromAPI: $groupId")
        try {
            val request = GroupOuterClass.GetGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .build()
            val response = dynamicAPIProvider.provideGroupBlockingStub().getGroup(request)

            return@withContext convertGroupFromResponse(response, signalGrpc, getDomain(), getClientId())
        } catch (e: Exception) {
            printlnCK("getGroupFromAPI error: $e")
            return@withContext null
        }
    }

    fun getTemporaryGroupWithAFriend(createPeople: User, receiverPeople: User): ChatGroup {
        return ChatGroup(
            id = GROUP_ID_TEMPO,
            groupName = receiverPeople.userName,
            groupAvatar = "",
            groupType = "peer",
            createBy = createPeople.id,
            createdAt = 0,
            updateBy = createPeople.id,
            updateAt = 0,
            rtcToken = "",
            clientList = listOf(createPeople, receiverPeople),

            isJoined = false,
            ownerDomain = createPeople.ownerDomain,
            ownerClientId = createPeople.id,

            lastMessage = null,
            lastMessageAt = 0,
            lastMessageSyncTimestamp = 0
        )
    }

    private suspend fun insertGroup(group: ChatGroup) {
        groupDAO.insert(group)
    }

    suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String) : ChatGroup? {
        printlnCK("getGroupByID: groupId = $groupId, $domain, owner = $ownerId")
        var room: ChatGroup? = groupDAO.getGroupById(groupId, domain, ownerId)
        if (room == null) {
            room = getGroupFromAPI(groupId, dynamicAPIProvider.provideSignalKeyDistributionBlockingStub())
            if (room != null) {
                insertGroup(room)
            }
        }
        return room
    }

    suspend fun getGroupByIDWithGrpc(groupId: Long, domain: String, ownerId: String,
                                     signalGrpc: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub
    ) : ChatGroup? {
        printlnCK("getGroupByIDWithGrpc: groupId = $groupId, $domain, owner = $ownerId")
        var room: ChatGroup? = groupDAO.getGroupById(groupId, domain, ownerId)
        if (room == null) {
            room = getGroupFromAPI(groupId, signalGrpc)
            if (room != null) {
                insertGroup(room)
            }
        }
        return room
    }

    suspend fun getGroupPeerByClientId(friend: User): ChatGroup? {
        return friend.let {
            groupDAO.getPeerGroups().firstOrNull {
                it.clientList.contains(friend)
            }
        }
    }

    suspend fun remarkGroupKeyRegistered(groupId: Long): ChatGroup {
        printlnCK("remarkGroupKeyRegistered, groupId = $groupId")
        val group = groupDAO.getGroupById(groupId, getDomain(), getClientId())!!
        val updateGroup = ChatGroup(
            id = group.id,
            groupName = group.groupName,
            groupAvatar = group.groupAvatar,
            groupType = group.groupType,
            createBy = group.createBy,
            createdAt = group.createdAt,
            updateBy = group.updateBy,
            updateAt = group.updateAt,
            rtcToken = group.rtcToken,
            clientList = group.clientList,

            // update
            isJoined = true,
            ownerDomain = group.ownerDomain,
            ownerClientId = group.ownerClientId,

            lastMessage = group.lastMessage,
            lastMessageAt = group.lastMessageAt,
            lastMessageSyncTimestamp = group.lastMessageSyncTimestamp
        )
        groupDAO.update(updateGroup)
        return updateGroup
    }

    suspend fun updateRoom(room: ChatGroup) = groupDAO.update(room)

    suspend fun convertGroupFromResponse(
        response: GroupOuterClass.GroupObjectResponse,
        signalGrpc: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        serverDomain: String,
        ownerId: String
    ): ChatGroup {
        val oldGroup = groupDAO.getGroupById(response.groupId, serverDomain, ownerId)
        val isRegisteredKey = oldGroup?.isJoined ?: false
        val lastMessageSyncTime = oldGroup?.lastMessageSyncTimestamp ?: environment.getServer().loginTime
        val domain = oldGroup?.ownerDomain ?: serverDomain
        val owner = oldGroup?.ownerClientId ?: ownerId

        val clientList = response.lstClientList.map {
            User(
                id = it.id,
                userName = it.displayName,
                ownerDomain = it.workspaceDomain,
            )
        }
        val groupName = if (isGroup(response.groupType)) response.groupName else {
            clientList?.firstOrNull { client ->
                client.id != owner
            }?.userName ?: ""
        }

        return ChatGroup(
            id = response.groupId,
            groupName = groupName,
            groupAvatar = response.groupAvatar,
            groupType = response.groupType,
            createBy = response.createdByClientId,
            createdAt = response.createdAt,
            updateBy = response.updatedByClientId,
            updateAt = response.updatedAt,
            rtcToken = response.groupRtcToken,
            clientList = clientList,
            isJoined = isRegisteredKey,
            ownerDomain = domain,
            ownerClientId = owner,
            lastMessage = null,
            lastMessageAt = response.lastMessageAt,
            lastMessageSyncTimestamp = lastMessageSyncTime
        )
    }
}
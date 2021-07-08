package com.clearkeep.screen.chat.repo

import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.printlnCK
import group.GroupGrpc
import group.GroupOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    // dao
    private val groupDAO: GroupDAO,

    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val apiProvider: ParamAPIProvider,

    private val serverRepository: ServerRepository,

    private val environment: Environment
) {
    fun getAllRooms() = groupDAO.getRoomsAsState()

    fun getClientId() = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    suspend fun fetchGroups() = withContext(Dispatchers.IO) {
        printlnCK("fetchGroups")
        val server = serverRepository.getServers()
        server?.forEach { server ->
            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val groupGrpc = apiProvider.provideGroupBlockingStub(paramAPI)
            try {
                val groups = getRoomsFromAPI(groupGrpc, server.profile.userId)
                for (group in groups) {
                    printlnCK("fetchGroups: $group")
                    val decryptedGroup = convertGroupFromResponse(group, server.serverDomain, server.profile.userId)
                    insertGroup(decryptedGroup)
                }
            } catch(exception: Exception) {
                printlnCK("fetchGroups: $exception")
            }
        }
    }

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
                    .setId(people.userId)
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

            val group = convertGroupFromResponse(response, getDomain(), getClientId())

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
        /*try {
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
        }*/
        return@withContext true
    }

    private suspend fun getGroupFromAPI(groupId: Long, groupGrpc: GroupGrpc.GroupBlockingStub, owner: Owner): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("getGroupFromAPI: $groupId")
        try {
            val request = GroupOuterClass.GetGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .build()
            val response = groupGrpc.getGroup(request)

            return@withContext convertGroupFromResponse(response, owner.domain, owner.clientId)
        } catch (e: Exception) {
            printlnCK("getGroupFromAPI error: $e")
            return@withContext null
        }
    }

    fun getTemporaryGroupWithAFriend(createPeople: User, receiverPeople: User): ChatGroup {
        return ChatGroup(
            groupId = GROUP_ID_TEMPO,
            groupName = receiverPeople.userName,
            groupAvatar = "",
            groupType = "peer",
            createBy = createPeople.userId,
            createdAt = 0,
            updateBy = createPeople.userId,
            updateAt = 0,
            rtcToken = "",
            clientList = listOf(createPeople, receiverPeople),

            isJoined = false,
            ownerDomain = createPeople.ownerDomain,
            ownerClientId = createPeople.userId,

            lastMessage = null,
            lastMessageAt = 0,
            lastMessageSyncTimestamp = 0
        )
    }

    private suspend fun insertGroup(group: ChatGroup) {
        groupDAO.insert(group)
    }

    suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String) : ChatGroup? {
        var room: ChatGroup? = groupDAO.getGroupById(groupId, domain, ownerId)
        if (room == null) {
            val server = serverRepository.getServer(domain, ownerId)
            if (server == null) {
                printlnCK("getGroupByID: null server")
                return null
            }
            val groupGrpc = apiProvider.provideGroupBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            room = getGroupFromAPI(groupId, groupGrpc, Owner(domain, ownerId))
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
        val group = groupDAO.getGroupById(groupId, getDomain(), getClientId())
        if (group == null) {
            printlnCK("remarkGroupKeyRegistered: can not find group with id = $groupId")
            throw IllegalArgumentException("can not find group with id = $groupId")
        }
        val updateGroup = ChatGroup(
            generateId = group.generateId,
            groupId = group.groupId,
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

    private suspend fun convertGroupFromResponse(
        response: GroupOuterClass.GroupObjectResponse,
        serverDomain: String,
        ownerId: String
    ): ChatGroup {
        val oldGroup = groupDAO.getGroupById(response.groupId, serverDomain, ownerId)
        val isRegisteredKey = oldGroup?.isJoined ?: false
        val lastMessageSyncTime = oldGroup?.lastMessageSyncTimestamp ?: environment.getServer().loginTime

        val clientList = response.lstClientList.map {
            User(
                userId = it.id,
                userName = it.displayName,
                ownerDomain = it.workspaceDomain,
            )
        }
        val groupName = if (isGroup(response.groupType)) response.groupName else {
            clientList?.firstOrNull { client ->
                client.userId != ownerId
            }?.userName ?: ""
        }

        return ChatGroup(
            generateId = oldGroup?.generateId,
            groupId = response.groupId,
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
            ownerDomain = serverDomain,
            ownerClientId = ownerId,
            lastMessage = null,
            lastMessageAt = response.lastMessageAt,
            lastMessageSyncTimestamp = lastMessageSyncTime
        )
    }
}
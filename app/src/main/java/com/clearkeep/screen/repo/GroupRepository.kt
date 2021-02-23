package com.clearkeep.screen.repo

import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.network.Resource
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.UserManager
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

        // network call
        private val groupBlockingStub: GroupGrpc.GroupBlockingStub,
        private val groupGrpc: GroupGrpc.GroupBlockingStub,
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,

        // data
        private val messageDAO: MessageDAO,
        private val userManager: UserManager,
        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
) {
    fun getAllRooms() = liveData {
        val disposable = emitSource(
                groupDAO.getRoomsAsState().map {
                    Resource.loading(it)
                }
        )
        try {
            val groups = getRoomsFromAPI()
            disposable.dispose()
            groupDAO.insertGroupList(groups)
            emitSource(
                    groupDAO.getRoomsAsState().map {
                        Resource.success(it)
                    }
            )
        } catch(exception: Exception) {
            printlnCK("getAllRooms: $exception")
            emitSource(
                    groupDAO.getRoomsAsState().map {
                        Resource.error(exception.toString(), it)
                    }
            )
        }
    }

    suspend fun fetchRoomsFromAPI() = withContext(Dispatchers.IO) {
        printlnCK("fetchRoomsFromAPI")
        try {
            val groups = getRoomsFromAPI()
            groupDAO.insertGroupList(groups)
        } catch(exception: Exception) {
            printlnCK("fetchRoomsFromAPI: $exception")
        }
    }

    @Throws(Exception::class)
    private suspend fun getRoomsFromAPI() : List<ChatGroup>  = withContext(Dispatchers.IO) {
        printlnCK("getRoomsFromAPI")
        val clientId = userManager.getClientId()
        val request = GroupOuterClass.GetJoinedGroupsRequest.newBuilder()
                .setClientId(clientId)
                .build()
        val response = groupGrpc.getJoinedGroups(request)
        printlnCK("getRoomsFromAPI, ${response.lstGroupList}")
        return@withContext response.lstGroupList
                .map { group ->
                    convertGroupFromResponse(group)
                }
    }

    suspend fun createGroupFromAPI(createClientId: String, groupName: String, participants: List<String>, isGroup: Boolean): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("createGroup: $groupName, clients $participants")
        try {
            val request = GroupOuterClass.CreateGroupRequest.newBuilder()
                    .setGroupName(groupName)
                    .setCreatedByClientId(createClientId)
                    .addAllLstClientId(participants)
                    .setGroupType(getGroupType(isGroup))
                    .build()
            val response = groupBlockingStub.createGroup(request)

            val group = convertGroupFromResponse(response)

            // save to database
            insertGroup(group)
            printlnCK("createGroup success")
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
            val response = groupBlockingStub.inviteToGroup(request)

            return@withContext response.success
        } catch (e: Exception) {
            printlnCK("inviteToGroup error: $e")
            return@withContext false
        }
    }

    suspend fun getGroupFromAPI(groupId: Long): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("getGroupFromAPI: $groupId")
        try {
            val request = GroupOuterClass.GetGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .build()
            val response = groupBlockingStub.getGroup(request)

            return@withContext convertGroupFromResponse(response)
        } catch (e: Exception) {
            printlnCK("getGroupFromAPI error: $e")
            return@withContext null
        }
    }

    fun getTemporaryGroupWithAFriend(createPeople: People, receiverPeople: People): ChatGroup {
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

                // TODO
                isJoined = false,
                lastMessage = null,
                lastMessageAt = 0
        )
    }

    suspend fun insertGroup(group: ChatGroup) {
        groupDAO.insert(group)
    }

    suspend fun getGroupByID(groupId: Long) = groupDAO.getGroupById(groupId)

    suspend fun getGroupPeerByClientId(friend: People): ChatGroup? {
        return friend.let {
            groupDAO.getPeerGroups().firstOrNull {
                it.clientList.contains(friend)
            }
        }
    }

    suspend fun remarkGroupKeyRegistered(groupId: Long) : Boolean {
        printlnCK("remarkGroupKeyRegistered, groupId = $groupId")
        val group = groupDAO.getGroupById(groupId)!!
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

                lastMessage = group.lastMessage,
                lastMessageAt = group.lastMessageAt
        )
        groupDAO.update(updateGroup)
        return true
    }

    suspend fun updateRoom(room: ChatGroup) = groupDAO.update(room)

    private suspend fun convertGroupFromResponse(response: GroupOuterClass.GroupObjectResponse): ChatGroup {
        val isRegisteredKey = groupDAO.getGroupById(response.groupId)?.isJoined ?: false
        return ChatGroup(
                id = response.groupId,
                groupName = response.groupName,
                groupAvatar = response.groupAvatar,
                groupType = response.groupType,
                createBy = response.createdByClientId,
                createdAt = response.createdAt,
                updateBy = response.updatedByClientId,
                updateAt = response.updatedAt,
                rtcToken = response.groupRtcToken,
                clientList = response.lstClientList.map {
                    People(
                        id = it.id,
                        userName = it.displayName
                    )
                },
                isJoined = isRegisteredKey,
                lastMessage = convertAndInsertLastMessageResponseFromGroup(
                        response.lastMessage, clientBlocking,
                        senderKeyStore, signalProtocolStore
                ),
                lastMessageAt = response.lastMessageAt
        )
    }

    private suspend fun convertAndInsertLastMessageResponseFromGroup(
            messageResponse: GroupOuterClass.MessageObjectResponse,

            clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
            senderKeyStore: InMemorySenderKeyStore,
            signalProtocolStore: InMemorySignalProtocolStore,
    ): Message? {
        if (messageResponse.id.isNullOrEmpty()) {
            return null
        }

        val oldMessage = messageDAO.getMessage(messageResponse.id)
        if (oldMessage != null) {
            return oldMessage
        }

        val decryptedMessage = try {
            if (!isGroup(messageResponse.groupType)) {
                decryptPeerMessage(messageResponse.fromClientId, messageResponse.message, signalProtocolStore)
            } else {
                decryptGroupMessage(messageResponse.fromClientId, messageResponse.groupId, messageResponse.message, senderKeyStore, clientBlocking)
            }
        } catch (e: Exception) {
            null
        }

        val newMessage = Message(
                messageResponse.id,
                messageResponse.groupId,
                messageResponse.groupType,
                messageResponse.fromClientId,
                messageResponse.clientId,
                decryptedMessage?: "",
                messageResponse.createdAt,
                messageResponse.updatedAt,
        )
        if (!decryptedMessage.isNullOrEmpty()) {
            messageDAO.insert(newMessage)
        }
        return newMessage
    }
}
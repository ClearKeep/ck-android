package com.clearkeep.screen.chat.repositories

import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.clearkeep.db.GroupDAO
import com.clearkeep.db.converter.SortedStringListConverter
import com.clearkeep.db.model.GROUP_ID_TEMPO
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.People
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.main.people.PeopleRepository
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.printlnCK
import group.GroupGrpc
import group.GroupOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
        private val roomDAO: GroupDAO,
        private val groupBlockingStub: GroupGrpc.GroupBlockingStub,
        private val groupGrpc: GroupGrpc.GroupBlockingStub,

        private val profileRepository: ProfileRepository,
        private val messageRepository: MessageRepository,

        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
) {
    fun getAllRooms() = liveData {
        val disposable = emitSource(
                roomDAO.getRoomsAsState().map {
                    Resource.loading(it)
                }
        )
        try {
            val groups = getRoomsFromAPI()
            disposable.dispose()
            roomDAO.insertGroupList(groups)
            emitSource(
                    roomDAO.getRoomsAsState().map {
                        Resource.success(it)
                    }
            )
        } catch(exception: Exception) {
            printlnCK("getAllRooms: $exception")
            emitSource(
                    roomDAO.getRoomsAsState().map {
                        Resource.error(exception.toString(), it)
                    }
            )
        }
    }

    suspend fun fetchRoomsFromAPI() = withContext(Dispatchers.IO) {
        printlnCK("fetchRoomsFromAPI")
        try {
            val groups = getRoomsFromAPI()
            roomDAO.insertGroupList(groups)
        } catch(exception: Exception) {
            printlnCK("fetchRoomsFromAPI: $exception")
        }
    }

    @Throws(Exception::class)
    private suspend fun getRoomsFromAPI() : List<ChatGroup>  = withContext(Dispatchers.IO) {
        printlnCK("getRoomsFromAPI")
        val clientId = profileRepository.getClientId()
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

    suspend fun inviteToGroupFromAPI(ourClientId: String, invitedFriendId: String, groupId: String): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun getGroupFromAPI(groupId: String): ChatGroup? = withContext(Dispatchers.IO) {
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
                clientList = listOf(createPeople, receiverPeople),

                // TODO
                isJoined = false,
                lastMessage = null,
                lastMessageAt = 0
        )
    }

    suspend fun insertGroup(group: ChatGroup) {
        roomDAO.insert(group)
    }

    suspend fun getGroupByID(groupId: String) = roomDAO.getGroupById(groupId)

    suspend fun getGroupPeerByClientId(friend: People): ChatGroup? {
        return friend?.let {
            roomDAO.getPeerGroups().firstOrNull {
                it.clientList.contains(friend)
            }
        }
    }

    suspend fun remarkJoinInRoom(groupId: String) : Boolean {
        val group = roomDAO.getGroupById(groupId)
        val updateGroup = ChatGroup(
                id = group.id,
                groupName = group.groupName,
                groupAvatar = group.groupAvatar,
                groupType = group.groupType,
                createBy = group.createBy,
                createdAt = group.createdAt,
                updateBy = group.updateBy,
                updateAt = group.updateAt,
                clientList = group.clientList,

                // update
                isJoined = true,

                lastMessage = group.lastMessage,
                lastMessageAt = group.lastMessageAt
        )
        roomDAO.update(updateGroup)
        return true
    }

    suspend fun updateRoom(room: ChatGroup) = roomDAO.update(room)

    private suspend fun convertGroupFromResponse(response: GroupOuterClass.GroupObjectResponse): ChatGroup {
        return ChatGroup(
                id = response.groupId,
                groupName = response.groupName,
                groupAvatar = response.groupAvatar,
                groupType = response.groupType,
                createBy = response.createdByClientId,
                createdAt = response.createdAt,
                updateBy = response.updatedByClientId,
                updateAt = response.updatedAt,
                clientList = response.lstClientList.map {
                    People(
                        id = it.id,
                        userName = it.username
                    )
                },

                // TODO
                isJoined = true,
                lastMessage = convertMessageResponseFromGroup(response.lastMessage, clientBlocking, senderKeyStore, signalProtocolStore, messageRepository),
                lastMessageAt = response.lastMessageAt
        )
    }

    private suspend fun convertMessageResponseFromGroup(
            messageResponse: GroupOuterClass.MessageObjectResponse,

            clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
            senderKeyStore: InMemorySenderKeyStore,
            signalProtocolStore: InMemorySignalProtocolStore,
            messageRepository: MessageRepository,
    ): Message? {
        if (messageResponse.id.isNullOrEmpty()) {
            return null
        }

        val oldMessage = messageRepository.getMessage(messageResponse.id)
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
            printlnCK("convertMessageResponseFromGroup error : $e")
            "error"
        }

        val newMessage = Message(
                messageResponse.id,
                messageResponse.groupId,
                messageResponse.groupType,
                messageResponse.fromClientId,
                messageResponse.clientId,
                decryptedMessage,
                messageResponse.createdAt,
                messageResponse.updatedAt,
        )
        messageRepository.insert(newMessage)
        return newMessage
    }
}
package com.clearkeep.screen.chat.repositories

import com.clearkeep.db.GroupDAO
import com.clearkeep.db.converter.SortedStringListConverter
import com.clearkeep.db.model.GROUP_ID_TEMPO
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.screen.chat.utils.getGroupType
import com.clearkeep.utilities.printlnCK
import group.GroupGrpc
import group.GroupOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
        private val roomDAO: GroupDAO,
        private val groupBlockingStub: GroupGrpc.GroupBlockingStub,
) {
    suspend fun createGroupFromAPI(createClientId: String, groupName: String, participants: List<String>, isGroup: Boolean): ChatGroup? = withContext(Dispatchers.IO) {
        printlnCK("createGroup: $groupName")
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
        printlnCK("inviteToGroup")
        try {
            val request = GroupOuterClass.InviteToGroupRequest.newBuilder()
                    .setFromClientId(ourClientId)
                    .setClientId(invitedFriendId)
                    .setGroupId(groupId)
                    .build()
            val response = groupBlockingStub.inviteToGroup(request)

            return@withContext true
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
            printlnCK("createGroup error: $e")
            return@withContext null
        }
    }

    fun getTemporaryGroupWithAFriend(createClientId: String, createClientName: String,
                                     friendID: String, friendName: String): ChatGroup {
        return ChatGroup(
                id = GROUP_ID_TEMPO,
                groupName = listOf(createClientName, friendName).joinToString (separator = ","),
                groupAvatar = "",
                groupType = "peer",
                createBy = createClientId,
                createdAt = 0,
                updateBy = createClientId,
                updateAt = 0,
                clientList = listOf(createClientId, friendID).sortedBy { it },

                // TODO
                isJoined = false,
                lastClient = "",
                lastMessage = "",
                lastUpdatedTime = 0
        )
    }

    suspend fun insertGroup(group: ChatGroup) {
        roomDAO.insert(group)
    }

    suspend fun getGroupByID(groupId: String) = roomDAO.getGroupById(groupId)

    suspend fun getGroupPeerByClientId(clientIds: List<String>): ChatGroup {
        val strSorted = SortedStringListConverter().saveList(clientIds)
        return roomDAO.getGroupPeerByClientId(strSorted)
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

                lastClient = group.lastClient,
                lastMessage = group.lastMessage,
                lastUpdatedTime = group.lastUpdatedTime
        )
        roomDAO.update(updateGroup)
        return true
    }

    suspend fun updateRoom(room: ChatGroup) = roomDAO.update(room)

    fun getAllRooms() = roomDAO.getRooms()

    private fun convertGroupFromResponse(response: GroupOuterClass.GroupObjectResponse): ChatGroup {
        return ChatGroup(
                id = response.groupId,
                groupName = response.groupName,
                groupAvatar = response.groupAvatar,
                groupType = response.groupType,
                createBy = response.createdByClientId,
                createdAt = response.createdAt,
                updateBy = response.updatedByClientId,
                updateAt = response.updatedAt,
                clientList = response.lstClientIdList,

                // TODO
                isJoined = false,
                lastClient = "",
                lastMessage = "",
                lastUpdatedTime = 0
        )
    }
}
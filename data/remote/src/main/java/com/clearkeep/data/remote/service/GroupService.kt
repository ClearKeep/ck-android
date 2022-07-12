package com.clearkeep.data.remote.service

import android.util.Log
import com.clearkeep.common.utilities.REQUEST_DEADLINE_SECONDS
import com.clearkeep.common.utilities.getGroupType
import com.clearkeep.data.remote.dynamicapi.DynamicAPIProvider
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.clearkeep.domain.model.User
import group.GroupOuterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserOuterClass
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GroupService @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val apiProvider: ParamAPIProvider
) {
    suspend fun getGroup(server: com.clearkeep.domain.model.Server, groupId: Long): GroupOuterClass.GroupObjectResponse = withContext(Dispatchers.IO) {
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
        val groupGrpc = apiProvider.provideGroupBlockingStub(paramAPI)
        val request = GroupOuterClass.GetGroupRequest.newBuilder()
            .setGroupId(groupId)
            .build()
        return@withContext groupGrpc.withDeadlineAfter(REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS)
            .getGroup(request)
    }

    suspend fun getJoinedGroups(server: com.clearkeep.domain.model.Server): GroupOuterClass.GetJoinedGroupsResponse = withContext(Dispatchers.IO) {
        val request = GroupOuterClass.GetJoinedGroupsRequest.newBuilder().build()
        return@withContext apiProvider.provideGroupBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        ).getJoinedGroups(request)
    }

    suspend fun createGroup(
        groupName: String,
        createdByClientId: String,
        users: List<User>,
        isGroup: Boolean
    ): GroupOuterClass.GroupObjectResponse = withContext(
        Dispatchers.IO
    ) {
        val groupType = getGroupType(isGroup)

        val clients = users.map { people ->
            GroupOuterClass.ClientInGroupObject.newBuilder()
                .setId(people.userId)
                .setDisplayName(people.userName)
                .setWorkspaceDomain(people.domain).build()
        }

        val request = GroupOuterClass.CreateGroupRequest.newBuilder()
            .setGroupName(groupName)
            .setCreatedByClientId(createdByClientId)
            .addAllLstClient(clients)
            .setGroupType(groupType)
            .build()

        return@withContext dynamicAPIProvider.provideGroupBlockingStub().createGroup(request)
    }

    suspend fun leaveGroup(groupId: Long, clientId: String, domain: String, displayName: String?): GroupOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val memberInfo = GroupOuterClass.MemberInfo.newBuilder()
            .setId(clientId)
            .setWorkspaceDomain(domain)
            .setDisplayName(displayName)
            .build()

        val request = GroupOuterClass.LeaveGroupRequest.newBuilder()
            .setLeaveMember(memberInfo)
            .setLeaveMemberBy(memberInfo)
            .setGroupId(groupId)
            .build()

        return@withContext dynamicAPIProvider.provideGroupBlockingStub().leaveGroup(request)
    }

    suspend fun removeMember(groupId: Long, removedUser: User, clientId: String, domain: String, userName: String?): GroupOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val memberInfo = GroupOuterClass.MemberInfo.newBuilder()
            .setId(removedUser.userId)
            .setWorkspaceDomain(removedUser.domain)
            .setDisplayName(removedUser.userName)
            .build()

        val removing = GroupOuterClass.MemberInfo.newBuilder()
            .setId(clientId)
            .setWorkspaceDomain(domain)
            .setDisplayName(userName)
            .build()

        val request = GroupOuterClass.LeaveGroupRequest.newBuilder()
            .setLeaveMember(memberInfo)
            .setLeaveMemberBy(removing)
            .setGroupId(groupId)
            .build()

        return@withContext dynamicAPIProvider.provideGroupBlockingStub().leaveGroup(request)
    }

    suspend fun addMember(groupId: Long, invitedUser: User, clientId: String, domain: String, userName: String?): GroupOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val memberInfo = GroupOuterClass.MemberInfo.newBuilder()
            .setId(invitedUser.userId)
            .setWorkspaceDomain(invitedUser.domain)
            .setDisplayName(invitedUser.userName)
            .setStatus("")
            .build()

        val adding = GroupOuterClass.MemberInfo.newBuilder()
            .setId(clientId)
            .setWorkspaceDomain(domain)
            .setDisplayName(userName)
            .build()

        val request = GroupOuterClass.AddMemberRequest.newBuilder()
            .setAddingMemberInfo(adding)
            .setAddedMemberInfo(memberInfo)
            .setGroupId(groupId)
            .build()

        return@withContext dynamicAPIProvider.provideGroupBlockingStub().addMember(request)
    }

    fun getUsersInServer(): UserOuterClass.GetUsersResponse {
        val request = UserOuterClass.Empty.newBuilder()
            .build()
        return dynamicAPIProvider.provideUserBlockingStub().getUsers(request)
    }

    suspend fun sendPing(): UserOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.PingRequest.newBuilder().build()
        return@withContext dynamicAPIProvider.provideUserBlockingStub().pingRequest(request)
    }

    suspend fun updateStatus(status: String?): UserOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.SetUserStatusRequest.newBuilder().setStatus(status).build()
        return@withContext dynamicAPIProvider.provideUserBlockingStub().updateStatus(request)
    }

    suspend fun getListClientStatus(users: List<User>): UserOuterClass.GetClientsStatusResponse = withContext(Dispatchers.IO) {
        val listMemberInfoRequest = users.map {
            UserOuterClass.MemberInfoRequest.newBuilder().setClientId(it.userId)
                .setWorkspaceDomain(it.domain).build()
        }

        val request = UserOuterClass.GetClientsStatusRequest
            .newBuilder()
            .addAllLstClient(listMemberInfoRequest)
            .setShouldGetProfile(true)
            .build()
        return@withContext dynamicAPIProvider.provideUserBlockingStub().getClientsStatus(request)
    }

    suspend fun getUserInfo(userId: String, userDomain: String): UserOuterClass.UserInfoResponse = withContext(Dispatchers.IO) {
        val request = UserOuterClass.GetUserRequest.newBuilder()
            .setClientId(userId)
            .setWorkspaceDomain(userDomain)
            .build()

        return@withContext dynamicAPIProvider.provideUserBlockingStub()
            .withDeadlineAfter(30, TimeUnit.SECONDS).getUserInfo(request)
    }

    suspend fun findUserByEmail(emailHard: String): List<User> = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass
                .FindUserByEmailRequest
                .newBuilder()
                .setEmailHash(emailHard)
                .build()
            val response = dynamicAPIProvider.provideUserBlockingStub().findUserByEmail(request)
            Log.e("hungnv", "findUserByEmail: response: $response")
            return@withContext response.lstUserOrBuilderList.map {
                Log.e("hungnv", "findUserByEmail: avatar ${it.avatar}" )
                User(it.id, it.displayName, it.workspaceDomain,avatar = it.avatar)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList<User>()
        }
    }
}
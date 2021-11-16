package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.data.remote.service.GroupService
import com.clearkeep.data.local.clearkeep.dao.GroupDAO
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.local.signal.dao.SignalIdentityKeyDAO
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.UserKeyRepository
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.GROUP_ID_TEMPO
import com.clearkeep.presentation.screen.chat.utils.isGroup
import com.clearkeep.presentation.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.fromHex
import com.clearkeep.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.utilities.network.Resource
import group.GroupOuterClass
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.ecc.ECPrivateKey
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.groups.SenderKeyName
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val groupDAO: GroupDAO,
    private val serverRepository: ServerRepository,
    private val environment: Environment,
    private val senderKeyStore: InMemorySenderKeyStore,
    private val userKeyRepository: UserKeyRepository,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,

    private val groupService: GroupService
) : GroupRepository {
    override fun getAllRooms() = groupDAO.getRoomsAsState()

    override fun getClientId(): String = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    override suspend fun fetchGroups() = withContext(Dispatchers.IO) {
        printlnCK("fetchGroups")
        val server = serverRepository.getServers()
        server.forEach { server ->
            try {
                val groups = getGroupListFromAPI(server.profile.userId)
                for (group in groups) {
                    printlnCK("fetchGroups ${group.groupName} ${group.lstClientList}")
                    val decryptedGroup =
                        convertGroupFromResponse(group, server.serverDomain, server.profile.userId)

                    val oldGroup = groupDAO.getGroupById(group.groupId, server.serverDomain)
                    if (oldGroup?.isDeletedUserPeer != true) {
                        insertGroup(decryptedGroup)
                    }
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("fetchGroups token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
            } catch (exception: Exception) {
                printlnCK("fetchGroups: $exception")
            }
        }
    }

    override suspend fun disableChatOfDeactivatedUser(clientId: String, domain: String, userId: String) {
        printlnCK("disableChatOfDeactivatedUser clientId $clientId domain $domain $userId")
        val peerRooms = groupDAO.getPeerGroups(domain, clientId).filter {
            printlnCK("disableChatOfDeactivatedUser peerRooms before filter $it")
            it.clientList.find { it.userId == userId } != null
        }.map { it.generateId ?: 0 }
        printlnCK("disableChatOfDeactivatedUser peerRooms $peerRooms")
        groupDAO.disableChatOfDeactivatedUser(peerRooms)
    }

    override suspend fun createGroup(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean
    ): Resource<ChatGroup>? = withContext(Dispatchers.IO) {
        try {
            val response = groupService.createGroup(groupName, createClientId, participants, isGroup)
            val group = convertGroupFromResponse(response, getDomain(), getClientId())

            // save to database
            insertGroup(group)
            printlnCK("createGroup success, $group")
            return@withContext Resource.success(group)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("createGroupFromAPI token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("createGroup error: $e")
            return@withContext null
        }
    }

    override suspend fun inviteToGroupFromAPIs(
        invitedUsers: List<User>,
        groupId: Long,
        owner: Owner
    ): Resource<ChatGroup> = withContext(Dispatchers.IO) {
        invitedUsers.forEach {
            printlnCK("inviteToGroupFromAPIs: ${it.userName}")
            inviteToGroupFromAPI(it, groupId, owner)
        }
        val server = serverRepository.getServer(domain = owner.domain, ownerId = owner.clientId)
        if (server == null) {
            printlnCK("fetchNewGroup: can not find server")
        }
        printlnCK("inviteToGroupFromAPIs")
        val group = getGroupFromAPI(groupId, owner)
        group.data?.let { insertGroup(it) }
        return@withContext group
    }

    override suspend fun removeMemberInGroup(removedUser: User, groupId: Long, owner: Owner): Boolean =
        withContext(Dispatchers.IO) {
            try {
                printlnCK("remoteMemberInGroup: remoteUser $removedUser groupId: $groupId")
                val response = groupService.removeMember(groupId, removedUser, getClientId(), owner.domain, environment.getServer().profile.userName)

                printlnCK("removeMemberInGroup: ${response.error}")
                return@withContext true
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("removeMemberInGroup token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
                return@withContext false
            } catch (e: Exception) {
                printlnCK("removeMemberInGroup: ${e.message}")
                e.printStackTrace()
            }
            return@withContext false
        }

    private suspend fun inviteToGroupFromAPI(
        invitedUser: User,
        groupId: Long,
        owner: Owner
    ): GroupOuterClass.BaseResponse? =
        withContext(Dispatchers.IO) {
            printlnCK("inviteToGroupFromAPI: $groupId ")
            try {
                val response = groupService.addMember(groupId, invitedUser, getClientId(), owner.domain, environment.getServer().profile.userName)
                printlnCK("inviteToGroupFromAPI: ${response.error}")
                return@withContext response
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("inviteToGroupFromAPI token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
                return@withContext null
            } catch (e: Exception) {
                printlnCK("inviteToGroupFromAPI error: $e")
                return@withContext null
            }
        }

    override suspend fun leaveGroup(groupId: Long, owner: Owner): Boolean =
        withContext(Dispatchers.IO) {
            try {
                printlnCK("leaveGroup groupId: groupId: $groupId ")
                val response = groupService.leaveGroup(groupId, getClientId(), owner.domain, environment.getServer().profile.userName)
                if (response.error.isNullOrEmpty()) {
                    getListClientInGroup(groupId, owner.domain)?.forEach {
                        val senderAddress2 = CKSignalProtocolAddress(
                            Owner(
                                owner.domain,
                                it
                            ), RECEIVER_DEVICE_ID
                        )
                        val senderAddress1 = CKSignalProtocolAddress(
                            Owner(
                                owner.domain,
                                it
                            ), SENDER_DEVICE_ID
                        )
                        val groupSender2 = SenderKeyName(groupId.toString(), senderAddress2)
                        val groupSender = SenderKeyName(groupId.toString(), senderAddress1)
                        senderKeyStore.deleteSenderKey(groupSender2)
                        senderKeyStore.deleteSenderKey(groupSender)
                    }
                    removeGroupOnWorkSpace(groupId, owner.domain, owner.clientId)
                    printlnCK("leaveGroup success: groupId: $groupId groupname: ${response.error}")
                    return@withContext true
                }
                return@withContext false
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("leaveGroup token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
                return@withContext false
            } catch (e: Exception) {
                e.printStackTrace()
                printlnCK("leaveGroup error: " + e.message.toString())
                return@withContext false
            }
        }

    private suspend fun getGroupFromAPI(
        groupId: Long,
        owner: Owner
    ): Resource<ChatGroup> = withContext(Dispatchers.IO) {
        printlnCK("getGroupFromAPI: $groupId")
        try {
            val server = serverRepository.getServer(domain = owner.domain, ownerId = owner.clientId)
            if (server == null) {
                printlnCK("fetchNewGroup: can not find server")
                return@withContext Resource.error("", null, 1001)
            }
            val response = groupService.getGroup(server, groupId)
            val group = convertGroupFromResponse(response, owner.domain, owner.clientId)
            printlnCK("getGroupFromAPI: ${group.clientList}")
            return@withContext Resource.success(group)
        } catch (e: StatusRuntimeException) {
            printlnCK("getGroupFromAPI: ${e.message.toString()}")
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("getGroupFromAPI token expired}")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("getGroupFromAPI error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun getGroupByGroupId(groupId: Long): ChatGroup? {
        return groupDAO.getGroupById(groupId, getDomain(), getClientId())
    }

    @Throws(Exception::class)
    private suspend fun getGroupListFromAPI(
        clientId: String
    ): List<GroupOuterClass.GroupObjectResponse> = withContext(Dispatchers.IO) {
        try {
            printlnCK("getGroupListFromAPI: $clientId")
            val server = serverRepository.getServer(getDomain(), clientId)
            if (server == null) {
                printlnCK("getGroupByID: null server")
                throw NullPointerException("getGroupListFromAPI null server")
            }
            val response = groupService.getJoinedGroups(server)
            return@withContext response.lstGroupList
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("getGroupListFromAPI token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    override fun getTemporaryGroupWithAFriend(createPeople: User, receiverPeople: User): ChatGroup {
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
            ownerDomain = createPeople.domain,
            ownerClientId = createPeople.userId,

            lastMessage = null,
            lastMessageAt = 0,
            lastMessageSyncTimestamp = 0,
            isDeletedUserPeer = false
        )
    }

    private suspend fun insertGroup(group: ChatGroup) {
        val oldGroup = groupDAO.getGroupById(group.groupId, group.ownerDomain)
        printlnCK("insertGroup called oldGroup $oldGroup")
        groupDAO.insert(group.copy(isDeletedUserPeer = oldGroup?.isDeletedUserPeer ?: false))
    }

    override suspend fun getGroupByID(groupId: Long, domain: String, ownerId: String): Resource<ChatGroup>? {
        printlnCK("getGroupByID: groupId $groupId")
        var room: Resource<ChatGroup>?

        val server = serverRepository.getServer(domain, ownerId)
        if (server == null) {
            printlnCK("getGroupByID: null server")
            return null
        }
        printlnCK("getGroupByID")
        room = getGroupFromAPI(groupId, Owner(domain, ownerId))
        if (room.data != null) {
            insertGroup(room.data!!)
        }
        return room
    }

    override suspend fun getGroupFromAPIById(
        groupId: Long,
        domain: String,
        ownerId: String
    ): Resource<ChatGroup>? {
        val server = serverRepository.getServer(domain, ownerId)
        return if (server == null) {
            printlnCK("getGroupByID: null server")
            null
        } else {
            printlnCK("getGroupFromAPIById")
            val room = getGroupFromAPI(groupId, Owner(domain, ownerId))
            if (room.data != null) {
                insertGroup(room.data)
            }
            room
        }
    }

    override suspend fun removeGroupOnWorkSpace(groupId: Long, domain: String, ownerClientId: String) {
        val result = groupDAO.deleteGroupById(groupId, domain, ownerClientId)

        if (result > 0) {
            printlnCK("removeGroupOnWorkSpace: groupId: $groupId")
        } else {
            printlnCK("removeGroupOnWorkSpace: groupId: $groupId fail")
        }
    }

    override suspend fun removeGroupByDomain(domain: String, ownerClientId: String) {
        val result = groupDAO.deleteGroupByOwnerDomain(domain, ownerClientId)
        printlnCK("removeGroupOnWorkSpace: groupId: $domain $result  $ownerClientId")
    }

    override suspend fun getGroupPeerByClientId(friend: User, owner: Owner): ChatGroup? {
        return friend.let {
            groupDAO.getPeerGroups(domain = owner.domain, ownerId = owner.clientId)
                .firstOrNull { group ->
                    group.clientList.firstOrNull { it.userId == friend.userId && it.domain == friend.domain } != null
                }
        }
    }

    override suspend fun remarkGroupKeyRegistered(groupId: Long): ChatGroup {
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
            lastMessageSyncTimestamp = group.lastMessageSyncTimestamp,
            isDeletedUserPeer = group.isDeletedUserPeer
        )
        groupDAO.updateGroup(updateGroup)
        return updateGroup
    }

    override fun getGroupsByGroupName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>> =
        groupDAO.getGroupsByGroupName(ownerDomain, ownerClientId, "%$query%")

    override fun getPeerRoomsByPeerName(ownerDomain: String, ownerClientId: String, query: String): LiveData<List<ChatGroup>> =
        groupDAO.getPeerRoomsByPeerName(ownerDomain, ownerClientId, "%$query%")

    override fun getGroupsByDomain(ownerDomain: String, ownerClientId: String): LiveData<List<ChatGroup>> =
        groupDAO.getGroupsByDomain(ownerDomain, ownerClientId)

    override suspend fun getAllPeerGroupByDomain(owner: Owner): List<ChatGroup> =
        groupDAO.getPeerGroups(owner.domain, owner.clientId)

    override suspend fun getListClientInGroup(
        groupId: Long,
        domain: String
    ): List<String>? {
        printlnCK(
            "getListClientInGroup: ${
                groupDAO.getGroupById(
                    groupId,
                    domain
                )?.clientList?.size
            }"
        )
        return groupDAO.getGroupById(groupId, domain)?.clientList?.map { it -> it.userId }
    }

    private suspend fun convertGroupFromResponse(
        response: GroupOuterClass.GroupObjectResponse,
        serverDomain: String,
        ownerId: String
    ): ChatGroup {
        val server = serverRepository.getServer(serverDomain, ownerId)
        val oldGroup = groupDAO.getGroupById(response.groupId, serverDomain, ownerId)
        var isRegisteredKey = oldGroup?.isJoined ?: false
        val lastMessageSyncTime =
            oldGroup?.lastMessageSyncTimestamp ?: server?.loginTime ?: getCurrentDateTime().time

        val clientList = response.lstClientList.map {
            User(
                userId = it.id,
                userName = it.displayName,
                domain = it.workspaceDomain,
                userState = it.status
            )
        }
        val groupName = if (isGroup(response.groupType)) response.groupName else {
            clientList.firstOrNull { client ->
                client.userId != ownerId
            }?.userName ?: ""
        }

        try {
            val senderAddress =
                CKSignalProtocolAddress(Owner(serverDomain, ownerId), SENDER_DEVICE_ID)
            val groupSender = SenderKeyName(response.groupId.toString(), senderAddress)
            if (response.clientKey.senderKeyId > 0 && response.groupType == "group" && !isRegisteredKey) {
                val senderKeyID = response.clientKey.senderKeyId
                val senderKey = response.clientKey.senderKey.toByteArray()
                val privateKeyEncrypt = response.clientKey.privateKey
                val userKey = userKeyRepository.get(serverDomain, ownerId)
                val identityKey = signalIdentityKeyDAO.getIdentityKey(ownerId, serverDomain)
                val privateKey = identityKey?.identityKeyPair?.privateKey

                val decryptor = DecryptsPBKDF2(toHex(privateKey!!.serialize()))
                val privateSenderKey = decryptor.decrypt(
                    fromHex(privateKeyEncrypt),
                    fromHex(userKey.salt),
                    fromHex(userKey.iv)
                )

                val eCPublicKey: ECPublicKey =
                    Curve.decodePoint(response.clientKey.publicKey.toByteArray(), 0)
                val eCPrivateKey: ECPrivateKey =
                    Curve.decodePrivatePoint(privateSenderKey)
                val identityKeyPair = ECKeyPair(eCPublicKey, eCPrivateKey)
                val senderKeyRecord = senderKeyStore.loadSenderKey(groupSender)

                senderKeyRecord.setSenderKeyState(
                    senderKeyID.toInt(),
                    0,
                    senderKey,
                    identityKeyPair
                )

                senderKeyStore.storeSenderKey(groupSender, senderKeyRecord)
                isRegisteredKey = true

            }
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("${e.printStackTrace()}")
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
            lastMessageSyncTimestamp = lastMessageSyncTime,
            isDeletedUserPeer = false
        )
    }
}
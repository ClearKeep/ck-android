package com.clearkeep.data.repository.group

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.clearkeep.common.utilities.*
import com.clearkeep.common.utilities.DecryptsPBKDF2.Companion.fromHex
import com.clearkeep.common.utilities.DecryptsPBKDF2.Companion.toHex
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.data.remote.service.GroupService
import com.clearkeep.data.local.clearkeep.group.GroupDAO
import com.clearkeep.data.local.clearkeep.userkey.UserKeyDAO
import com.clearkeep.data.local.signal.identitykey.SignalIdentityKeyDAO
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.common.utilities.network.TokenExpiredException
import com.clearkeep.data.local.clearkeep.userkey.UserKeyEntity
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.response.GroupObjectResponse
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.repository.SignalKeyRepository
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
    private val signalKeyRepository: SignalKeyRepository,
    private val userKeyDAO: UserKeyDAO,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val environment: Environment,
    private val groupService: GroupService
) : GroupRepository {
    override fun getAllRoomsAsState() = groupDAO.getRoomsAsState().map { it.map { it.toModel() } }

    override suspend fun getAllRooms(): List<ChatGroup> = withContext(Dispatchers.IO) {
        return@withContext groupDAO.getRooms().map { it.toModel() }
    }

    private fun getClientId(): String = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    override suspend fun setDeletedUserPeerGroup(peerRoomsId: List<Int>) =
        withContext(Dispatchers.IO) {
            return@withContext groupDAO.setDeletedUserPeerGroup(peerRoomsId)
        }

    override suspend fun createGroup(
        createClientId: String,
        groupName: String,
        participants: MutableList<User>,
        isGroup: Boolean,
        domain: String,
        clientId: String,
        server: Server?
    ): Resource<GroupObjectResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    groupService.createGroup(groupName, createClientId, participants, isGroup)
                return@withContext Resource.success(response.toEntity())
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return@withContext Resource.error(parsedError.message, null, parsedError.code)
            } catch (e: Exception) {
                printlnCK("createGroup error: $e")
                return@withContext Resource.error(e.message ?: "", null, error = e)
            }
        }

    override suspend fun updateGroup(group: ChatGroup) = withContext(Dispatchers.IO) {
        groupDAO.updateGroup(group.toEntity())
    }

    override suspend fun removeMemberInGroup(
        removedUser: User,
        groupId: Long,
        owner: Owner
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                printlnCK("remoteMemberInGroup: remoteUser $removedUser groupId: $groupId")
                val response = groupService.removeMember(
                    groupId,
                    removedUser,
                    getClientId(),
                    owner.domain,
                    environment.getServer().profile.userName
                )
                printlnCK("removeMemberInGroup: ${response.error}")
                return@withContext true
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return@withContext false
            } catch (e: Exception) {
                printlnCK("removeMemberInGroup: ${e.message}")
                e.printStackTrace()
            }
            return@withContext false
        }

    override suspend fun inviteUserToGroup(
        invitedUser: User,
        groupId: Long,
        owner: Owner
    ): String? =
        withContext(Dispatchers.IO) {
            printlnCK("inviteToGroupFromAPI: $groupId ")
            try {
                val response = groupService.addMember(
                    groupId,
                    invitedUser,
                    getClientId(),
                    owner.domain,
                    environment.getServer().profile.userName
                )
                printlnCK("inviteToGroupFromAPI: ${response.error}")
                return@withContext response.error
            } catch (e: StatusRuntimeException) {
                return@withContext null
            } catch (e: Exception) {
                printlnCK("inviteToGroupFromAPI error: $e")
                return@withContext null
            }
        }

    override suspend fun leaveGroup(groupId: Long, owner: Owner): String? =
        withContext(Dispatchers.IO) {
            try {
                printlnCK("leaveGroup groupId: groupId: $groupId ")
                return@withContext groupService.leaveGroup(
                    groupId,
                    getClientId(),
                    owner.domain,
                    environment.getServer().profile.userName
                ).error
            } catch (e: StatusRuntimeException) {
                return@withContext null
            } catch (e: Exception) {
                e.printStackTrace()
                printlnCK("leaveGroup error: " + e.message.toString())
                return@withContext null
            }
        }

    override suspend fun getGroup(
        groupId: Long,
        owner: Owner,
        server: Server?
    ): Resource<ChatGroup> = withContext(Dispatchers.IO) {
        try {
            if (server == null) {
                printlnCK("fetchNewGroup: can not find server")
                return@withContext Resource.error("", null, 1001)
            }
            val rawResponse = groupService.getGroup(server, groupId)
            val group = convertGroupFromResponse(
                rawResponse.toEntity(),
                owner.domain,
                owner.clientId,
                server
            )
            return@withContext Resource.success(group)
        } catch (e: StatusRuntimeException) {
            printlnCK("getGroup: ${e.message.toString()}")
            val parsedError = parseError(e)
            if (parsedError.code == 1018) {
                return@withContext Resource.error(
                    parsedError.message,
                    null,
                    parsedError.code,
                    TokenExpiredException()
                )
            }
            return@withContext Resource.error(parsedError.message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("getGroup error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun getGroupByGroupId(groupId: Long): ChatGroup? =
        withContext(Dispatchers.IO) {
            return@withContext groupDAO.getGroupById(groupId, getDomain(), getClientId())
                ?.toModel()
        }

    override suspend fun fetchGroups(
        server: Server
    ): Resource<List<GroupObjectResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = groupService.getJoinedGroups(server)
                return@withContext Resource.success(response.lstGroupList.map { it.toEntity() })
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return@withContext Resource.error(
                    parsedError.message,
                    null,
                    parsedError.code,
                    parsedError.cause
                )
            } catch (e: Exception) {
                return@withContext Resource.error(e.message ?: "", null, 0, e)
            }
        }

    override suspend fun insertGroup(group: ChatGroup): Unit = withContext(Dispatchers.IO) {
        val oldGroup = groupDAO.getGroupById(group.groupId, group.ownerDomain)
        groupDAO.insert(
            group.copy(isDeletedUserPeer = oldGroup?.isDeletedUserPeer ?: false).toEntity()
        )
    }

    override suspend fun getGroupByID(
        groupId: Long,
        domain: String,
        ownerId: String,
        server: Server?,
        forceRefresh: Boolean
    ): Resource<ChatGroup> = withContext(Dispatchers.IO) {
        return@withContext if (forceRefresh) {
            val room: Resource<ChatGroup> = getGroup(
                groupId,
                Owner(
                    server?.serverDomain ?: "",
                    server?.ownerClientId ?: ""
                ), server
            )
            if (room.data != null) {
                insertGroup(room.data!!)
            }
            room
        } else {
            Resource.success(groupDAO.getGroupById(groupId, domain, ownerId)?.toModel())
        }
    }

    override suspend fun getGroupByID(groupId: Long, serverDomain: String): ChatGroup? =
        withContext(Dispatchers.IO) {
            return@withContext groupDAO.getGroupById(groupId, serverDomain)?.toModel()
        }

    override suspend fun deleteGroup(groupId: Long, domain: String, ownerClientId: String): Unit =
        withContext(Dispatchers.IO) {
            groupDAO.deleteGroupById(groupId, domain, ownerClientId)
        }

    override suspend fun deleteGroup(domain: String, ownerClientId: String): Unit =
        withContext(Dispatchers.IO) {
            groupDAO.deleteGroupByOwnerDomain(domain, ownerClientId)
        }

    override suspend fun getGroupPeerByClientId(friend: User, owner: Owner): ChatGroup? =
        withContext(Dispatchers.IO) {
            return@withContext friend.let {
                groupDAO.getPeerGroups(domain = owner.domain, ownerId = owner.clientId)
                    .firstOrNull { group ->
                        group.clientList.firstOrNull { it.userId == friend.userId && it.domain == friend.domain } != null
                    }?.toModel()
            }
        }

    override fun getGroupsByGroupName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>> =
        groupDAO.getGroupsByGroupName(ownerDomain, ownerClientId, "%$query%")
            .map { it.map { it.toModel() } }

    override fun getPeerRoomsByPeerName(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<ChatGroup>> =
        groupDAO.getPeerRoomsByPeerName(ownerDomain, ownerClientId, "%$query%")
            .map { it.map { it.toModel() } }

    override fun getGroupsByDomain(
        ownerDomain: String,
        ownerClientId: String
    ): LiveData<List<ChatGroup>> =
        groupDAO.getGroupsByDomain(ownerDomain, ownerClientId).map { it.map { it.toModel() } }

    override suspend fun getAllPeerGroupByDomain(owner: Owner): List<ChatGroup> =
        withContext(Dispatchers.IO) {
            groupDAO.getPeerGroups(owner.domain, owner.clientId).map { it.toModel() }
        }

    override suspend fun getListClientInGroup(
        groupId: Long,
        domain: String
    ): List<String>? = withContext(Dispatchers.IO) {
        return@withContext groupDAO.getGroupById(
            groupId,
            domain
        )?.clientList?.map { it -> it.userId }
    }

    override suspend fun convertGroupFromResponse(
        response: GroupObjectResponse,
        serverDomain: String,
        ownerId: String,
        server: Server?
    ): ChatGroup = withContext(Dispatchers.IO) {
        val oldGroup = groupDAO.getGroupById(response.groupId, serverDomain, ownerId)
        var isRegisteredKey = oldGroup?.isJoined ?: false
        Log.d("antx: ", "GroupRepositoryImpl convertGroupFromResponse line = 316:$isRegisteredKey " );
        val lastMessageSyncTime =
            oldGroup?.lastMessageSyncTimestamp ?: server?.loginTime ?: getCurrentDateTime().time

        val clientList = response.lstClient.map {
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
            Log.d("antx: ", "GroupRepositoryImpl convertGroupFromResponse line = 337:isRegisteredKey $isRegisteredKey " );
            if (response.clientKey.senderKeyId > 0 && response.groupType == "group" && !isRegisteredKey) {
                Log.d("antx: ", "GroupRepositoryImpl convertGroupFromResponse line = 337: " );
                val senderKeyID = response.clientKey.senderKeyId
                val senderKey = response.clientKey.senderKey
                val privateKeyEncrypt = response.clientKey.privateKey
                val userKey =
                    userKeyDAO.getKey(serverDomain, ownerId) ?: UserKeyEntity(
                        serverDomain,
                        ownerId,
                        "",
                        ""
                    )
                val identityKey = signalIdentityKeyDAO.getIdentityKey(ownerId, serverDomain)
                val privateKey = identityKey?.identityKeyPair?.privateKey

                val decryptor = DecryptsPBKDF2(toHex(privateKey!!.serialize()))
                printlnCK("convertGroupFromResponse PK ${toHex(privateKey.serialize())} salt ${userKey.salt} iv ${userKey.iv}")
                val privateSenderKey = decryptor.decrypt(
                    fromHex(privateKeyEncrypt),
                    fromHex(userKey.salt),
                    fromHex(userKey.iv)
                )

                val eCPublicKey: ECPublicKey =
                    Curve.decodePoint(response.clientKey.publicKey, 0)
                val eCPrivateKey: ECPrivateKey =
                    Curve.decodePrivatePoint(privateSenderKey)
                val identityKeyPair = ECKeyPair(eCPublicKey, eCPrivateKey)
                val senderKeyRecord = signalKeyRepository.loadSenderKey(groupSender)

                senderKeyRecord.setSenderKeyState(
                    senderKeyID.toInt(),
                    0,
                    senderKey,
                    identityKeyPair
                )

                signalKeyRepository.storeSenderKey(groupSender, senderKeyRecord)
                isRegisteredKey = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("${e.printStackTrace()}")
        }

        return@withContext ChatGroup(
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
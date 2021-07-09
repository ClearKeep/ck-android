package com.clearkeep.screen.chat.room

import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import javax.inject.Inject

class RoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,

    private val peopleRepository: PeopleRepository,
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,

    private val environment: Environment
): ViewModel() {
    private var roomId: Long? = null

    private var friendId: String? = null

    private var isLatestPeerSignalKeyProcessed = false

    private val _group = MutableLiveData<ChatGroup>()

    private val _requestCallState = MutableLiveData<Resource<RequestInfo>>()

    val requestCallState: LiveData<Resource<RequestInfo>>
        get() = _requestCallState

    val group: LiveData<ChatGroup>
        get() = _group

    var clientId: String = ""

    var domain: String = ""

    fun joinRoom(ownerDomain: String, ownerClientId: String, roomId: Long?, friendId: String?, friendDomain: String?) {
        if (ownerDomain.isNullOrBlank() || ownerClientId.isNullOrBlank()) {
            throw IllegalArgumentException("domain and clientId must be not NULL")
        }
        if (roomId == null && TextUtils.isEmpty(friendId)) {
            throw IllegalArgumentException("Can not load room with both groupId and friendId is empty")
        }
        this.domain = ownerDomain
        this.clientId = ownerClientId
        this.roomId = roomId
        this.friendId = friendId

        viewModelScope.launch {
            val selectedServer = serverRepository.getServerByOwner(Owner(ownerDomain, ownerClientId))
            if (selectedServer == null) {
                printlnCK("default server must be not NULL")
                return@launch
            }
            environment.setUpDomain(selectedServer)
            serverRepository.setActiveServer(selectedServer)

            if (roomId != null && roomId != 0L) {
                updateGroupWithId(roomId)
            } else if (!friendId.isNullOrEmpty() && !friendDomain.isNullOrEmpty()) {
                updateGroupWithFriendId(friendId, friendDomain)
            }
        }
    }

    fun leaveRoom() {
        setJoiningRoomId(-1)
    }

    fun setJoiningRoomId(roomId: Long) {
        chatRepository.setJoiningRoomId(roomId)
    }

    fun getMessages(groupId: Long, domain: String, clientId: String): LiveData<List<Message>> {
        printlnCK("getMessages: groupId $groupId")
        return messageRepository.getMessagesAsState(groupId, Owner(domain, clientId))
    }

    private suspend fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        val ret = groupRepository.getGroupByID(groupId, domain, clientId)
        if (ret != null) {
            setJoiningGroup(ret)
            updateMessagesFromRemote(groupId, ret.lastMessageSyncTimestamp)
        }
    }

    private suspend fun updateGroupWithFriendId(friendId: String, friendDomain: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        val friend = peopleRepository.getFriend(friendId, friendDomain, getOwner()) ?: User(userId = friendId, userName = "", domain = friendDomain)
        if (friend == null) {
            printlnCK("updateGroupWithFriendId: can not find friend with id $friendId")
            return
        }
        var existingGroup = groupRepository.getGroupPeerByClientId(friend, Owner(domain = domain, clientId = clientId))
        if (existingGroup == null) {
            existingGroup = groupRepository.getTemporaryGroupWithAFriend(getUser(), friend)
        } else {
            updateMessagesFromRemote(existingGroup.groupId, existingGroup.lastMessageSyncTimestamp)
        }
        setJoiningGroup(existingGroup)
    }

    private fun getOwner(): Owner {
        return Owner(domain, clientId)
    }

    private suspend fun updateMessagesFromRemote(groupId: Long, lastMessageAt: Long) {
        val server = environment.getServer()
        messageRepository.updateMessageFromAPI(groupId, Owner(server.serverDomain, server.profile.userId), lastMessageAt, 0)
    }

    fun sendMessageToUser(receiverPeople: User, groupId: Long, message: String) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = environment.getServer().profile
                val group = groupRepository.createGroupFromAPI(
                        user.userId,
                        "${user.getDisplayName()},${receiverPeople.userName}",
                        mutableListOf(getUser(), receiverPeople),
                        false
                )
                if (group != null) {
                    setJoiningGroup(group)
                    lastGroupId = group.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                if (!isLatestPeerSignalKeyProcessed) {
                    // work around: always load user signal key for first open room
                    val success = chatRepository.sendMessageInPeer(clientId, domain, receiverPeople.userId, receiverPeople.domain, lastGroupId, message, isForceProcessKey = true)
                    isLatestPeerSignalKeyProcessed = success
                } else {
                    chatRepository.sendMessageInPeer(clientId, domain, receiverPeople.userId, receiverPeople.domain, lastGroupId, message)
                }
            }
        }
    }

    fun sendMessageToGroup(groupId: Long, message: String, isRegisteredGroup: Boolean) {
        viewModelScope.launch {
            try {
                if (!isRegisteredGroup) {
                    val result = signalKeyRepository.registerSenderKeyToGroup(groupId, clientId, domain)
                    if (result) {
                        _group.value = groupRepository.remarkGroupKeyRegistered(groupId)
                        chatRepository.sendMessageToGroup(clientId, domain, groupId, message)
                    }
                } else {
                    chatRepository.sendMessageToGroup(clientId, domain, groupId, message)
                }
            } catch (e : Exception) {}
        }
    }

    fun inviteToGroup(invitedFriendId: String, groupId: Long) {
        viewModelScope.launch {
            groupRepository.inviteToGroupFromAPI(clientId, invitedFriendId, groupId)
        }
    }

    fun requestCall(groupId: Long, isAudioMode: Boolean) {
        viewModelScope.launch {
            _requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = getUser()
                val friend = peopleRepository.getFriend(friendId!!, domain, getOwner())!!
                val group = groupRepository.createGroupFromAPI(
                    user.userId,
                    "",
                    mutableListOf(user, friend),
                        false
                )
                if (group != null) {
                    setJoiningGroup(group)
                    lastGroupId = group.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                _requestCallState.value = Resource.success(_group.value?.let { RequestInfo(it, isAudioMode) })
            } else {
                _requestCallState.value = Resource.error("error",
                    _group.value?.let { RequestInfo(it, isAudioMode) })
            }
        }
    }

    private fun setJoiningGroup(group: ChatGroup) {
        _group.value = group
        chatRepository.setJoiningRoomId(group.groupId)
    }

    private fun getUser(): User {
        val server = environment.getServer()
        return User(userId = server.profile.userId, userName = server.profile.getDisplayName(), domain = server.serverDomain)
    }
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)
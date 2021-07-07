package com.clearkeep.screen.chat.room

import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
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

    fun getClientId() = environment.getServer().profile.id

    private fun getDomain() = environment.getServer().serverDomain

    fun joinRoom(roomId: Long?, friendId: String?) {
        if (roomId == null && TextUtils.isEmpty(friendId)) {
            throw IllegalArgumentException("Can not load room with both groupId and friendId is empty")
        }
        this.roomId = roomId
        this.friendId = friendId
        if (roomId != null && roomId != 0L) {
            updateGroupWithId(roomId)
        } else if (!friendId.isNullOrEmpty()) {
            updateGroupWithFriendId(friendId)
        }
    }

    fun leaveRoom() {
        setJoiningRoomId(-1)
    }

    fun setJoiningRoomId(roomId: Long) {
        chatRepository.setJoiningRoomId(roomId)
    }

    fun getMessages(groupId: Long): LiveData<List<Message>> {
        return messageRepository.getMessagesAsState(groupId, getOwner())
    }

    private fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        viewModelScope.launch {
            val ret = groupRepository.getGroupByID(groupId, getDomain(), getClientId())
            if (ret != null) {
                _group.value = ret
                updateMessagesFromRemote(groupId, ret.lastMessageSyncTimestamp)
            }
        }
    }

    private fun updateGroupWithFriendId(friendId: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        viewModelScope.launch {
            val friend = peopleRepository.getFriend(friendId) ?: User(friendId, "", "",)
            var existingGroup = groupRepository.getGroupPeerByClientId(friend)
            if (existingGroup == null) {
                existingGroup = groupRepository.getTemporaryGroupWithAFriend(getUser(), friend)
            } else {
                updateMessagesFromRemote(existingGroup.groupId, existingGroup.lastMessageSyncTimestamp)
            }
            _group.value = existingGroup
        }
    }

    private suspend fun updateMessagesFromRemote(groupId: Long, lastMessageAt: Long) {
        val server = environment.getServer()
        messageRepository.updateMessageFromAPI(groupId, Owner(server.serverDomain, server.profile.id), lastMessageAt, 0)
    }

    fun sendMessageToUser(receiverPeople: User, groupId: Long, message: String) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = environment.getServer().profile
                val group = groupRepository.createGroupFromAPI(
                        user.id,
                        "${user.getDisplayName()},${receiverPeople.userName}",
                        mutableListOf(getUser(), receiverPeople),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                if (!isLatestPeerSignalKeyProcessed) {
                    // work around: always load user signal key for first open room
                    val success = chatRepository.sendMessageInPeer(getClientId(), getDomain(), receiverPeople.id, receiverPeople.ownerDomain, lastGroupId, message, isForceProcessKey = true)
                    isLatestPeerSignalKeyProcessed = success
                } else {
                    chatRepository.sendMessageInPeer(getClientId(), getDomain(), receiverPeople.id, receiverPeople.ownerDomain, lastGroupId, message)
                }
            }
        }
    }

    fun sendMessageToGroup(groupId: Long, message: String, isRegisteredGroup: Boolean) {
        viewModelScope.launch {
            try {
                if (!isRegisteredGroup) {
                    val result = signalKeyRepository.registerSenderKeyToGroup(groupId, getClientId(), getDomain())
                    if (result) {
                        _group.value = groupRepository.remarkGroupKeyRegistered(groupId)
                        chatRepository.sendMessageToGroup(getClientId(), getDomain(), groupId, message)
                    }
                } else {
                    chatRepository.sendMessageToGroup(getClientId(), getDomain(), groupId, message)
                }
            } catch (e : Exception) {}
        }
    }

    fun inviteToGroup(invitedFriendId: String, groupId: Long) {
        viewModelScope.launch {
            groupRepository.inviteToGroupFromAPI(getClientId(), invitedFriendId, groupId)
        }
    }

    fun requestCall(groupId: Long, isAudioMode: Boolean) {
        viewModelScope.launch {
            _requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = getUser()
                val friend = peopleRepository.getFriend(friendId!!)!!
                val group = groupRepository.createGroupFromAPI(
                    user.id,
                    "",
                    mutableListOf(user, friend),
                        false
                )
                if (group != null) {
                    _group.value = group
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

    private fun getUser(): User {
        val server = environment.getServer()
        return User(server.profile.id, server.profile.getDisplayName(), server.serverDomain)
    }

    private fun getOwner(): Owner {
        val server = environment.getServer()
        return Owner(server.serverDomain, server.profile.id)
    }
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)
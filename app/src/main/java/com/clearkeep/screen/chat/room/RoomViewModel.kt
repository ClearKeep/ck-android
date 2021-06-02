package com.clearkeep.screen.chat.room

import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.*
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import javax.inject.Inject

class RoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val roomRepository: GroupRepository,
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,

    private val userManager: UserManager,

    private val peopleRepository: PeopleRepository,
    private val messageRepository: MessageRepository,
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
        return messageRepository.getMessagesAsState(groupId)
    }

    fun getClientId() = chatRepository.getClientId()

    private fun getUserName() = userManager.getUserName()

    private fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        viewModelScope.launch {
            val ret = roomRepository.getGroupByID(groupId)
            if (ret != null) {
                _group.value = ret
                updateMessagesFromRemote(groupId, ret.lastMessageSyncTimestamp)
            }
        }
    }

    private fun updateGroupWithFriendId(friendId: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        viewModelScope.launch {
            val friend = peopleRepository.getFriend(friendId) ?: People(friendId, "", "")
            var existingGroup = roomRepository.getGroupPeerByClientId(friend)
            if (existingGroup == null) {
                existingGroup = roomRepository.getTemporaryGroupWithAFriend(
                    People(getClientId(), getUserName(), ""),
                    People(friendId, friend.userName, "")
                )
            } else {
                updateMessagesFromRemote(existingGroup.id, existingGroup.lastMessageSyncTimestamp)
            }
            _group.value = existingGroup
        }
    }

    private suspend fun updateMessagesFromRemote(groupId: Long, lastMessageAt: Long) {
        messageRepository.updateMessageFromAPI(groupId, lastMessageAt, 0)
    }

    fun sendMessageToUser(receiverPeople: People, groupId: Long, message: String) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = userManager.getUser()
                val group = roomRepository.createGroupFromAPI(
                        getClientId(),
                        "${getUserName()},${receiverPeople.userName}",
                        mutableListOf(user, receiverPeople),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.id
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                if (!isLatestPeerSignalKeyProcessed) {
                    // work around: always load user signal key for first open room
                    val success = chatRepository.sendMessageInPeer(receiverPeople.id, lastGroupId, message, isForceProcessKey = true)
                    isLatestPeerSignalKeyProcessed = success
                } else {
                    chatRepository.sendMessageInPeer(receiverPeople.id, lastGroupId, message)
                }
            }
        }
    }

    fun sendMessageToGroup(groupId: Long, message: String, isRegisteredGroup: Boolean) {
        viewModelScope.launch {
            try {
                if (!isRegisteredGroup) {
                    val result = signalKeyRepository.registerSenderKeyToGroup(groupId, getClientId())
                    if (result) {
                        _group.value = roomRepository.remarkGroupKeyRegistered(groupId)
                        chatRepository.sendMessageToGroup(groupId, message)
                    }
                } else {
                    chatRepository.sendMessageToGroup(groupId, message)
                }
            } catch (e : Exception) {}
        }
    }

    fun inviteToGroup(invitedFriendId: String, groupId: Long) {
        viewModelScope.launch {
            groupRepository.inviteToGroupFromAPI(getClientId(),invitedFriendId, groupId)
        }
    }

    fun requestCall(groupId: Long, isAudioMode: Boolean) {
        viewModelScope.launch {
            _requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = userManager.getUser()
                val friend = peopleRepository.getFriend(friendId!!)!!
                val group = roomRepository.createGroupFromAPI(
                        getClientId(),
                        "",
                    mutableListOf(user, friend),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.id
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
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)
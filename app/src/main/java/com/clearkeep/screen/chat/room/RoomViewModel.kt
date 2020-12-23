package com.clearkeep.screen.chat.room

import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.GROUP_ID_TEMPO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.repo.*
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
        private val videoCallRepository: VideoCallRepository,

        private val userManager: UserManager,

        private val peopleRepository: PeopleRepository,
        private val messageRepository: MessageRepository
): ViewModel() {
    private var roomId: Long? = null

    private var friendId: String? = null

    private val _group = MutableLiveData<ChatGroup>()

    private val _requestCallState = MutableLiveData<Resource<ChatGroup>>()

    val requestCallState: LiveData<Resource<ChatGroup>>
        get() = _requestCallState

    private var isGroupRegistered: Boolean? = null

    val group: LiveData<ChatGroup>
        get() = _group

    val members: LiveData<List<People>> = _group.switchMap { group ->
        liveData(context = viewModelScope.coroutineContext) {
            emit(peopleRepository.getFriends(group.clientList.map { it.id }))
        }
    }

    fun initRoom(roomId: Long?, friendId: String?) {
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

    fun getMessages(groupId: Long): LiveData<List<Message>> {
        return messageRepository.getMessagesAsState(groupId)
    }

    fun getClientId() = chatRepository.getClientId()

    private fun getUserName() = userManager.getUserName()

    private fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        viewModelScope.launch {
            val ret = roomRepository.getGroupByID(groupId)
            _group.value = ret
            updateMessagesFromRemote(groupId, ret.lastMessageAt)
        }
    }

    private fun updateGroupWithFriendId(friendId: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        viewModelScope.launch {
            val friend = peopleRepository.getFriend(friendId) ?: People(friendId, "")
            var existingGroup = roomRepository.getGroupPeerByClientId(friend)
            if (existingGroup == null) {
                existingGroup = roomRepository.getTemporaryGroupWithAFriend(
                    People(getClientId(), getUserName()),
                    People(friendId, friend.userName)
                )
            } else {
                updateMessagesFromRemote(existingGroup.id, existingGroup.lastMessageAt)
            }
            _group.value = existingGroup
        }
    }

    private suspend fun updateMessagesFromRemote(groupId: Long, lastMessageAt: Long) {
        val messages = messageRepository.getMessages(groupId)
        val preLastMessage = messages.takeLast(2).firstOrNull()
        if (preLastMessage != null && preLastMessage.createdTime > 0 && preLastMessage.createdTime < lastMessageAt) {
            printlnCK("load message: from time $preLastMessage to $lastMessageAt")
            messageRepository.fetchMessageFromAPI(groupId, preLastMessage.createdTime, 0)
        }
    }

    fun sendMessageToUser(receiverPeople: People, groupId: Long, message: String) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val group = roomRepository.createGroupFromAPI(
                        getClientId(),
                        "${getUserName()},${receiverPeople.userName}",
                        listOf(getClientId(), receiverPeople.id),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.id
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                chatRepository.sendMessageInPeer(receiverPeople.id, lastGroupId, message)
            }
        }
    }

    fun sendMessageToGroup(groupId: Long, message: String, isRegisteredGroup: Boolean) {
        viewModelScope.launch {
            try {
                if (isGroupRegistered == null) {
                    isGroupRegistered = signalKeyRepository.isRegisteredGroupKey(groupId, getClientId())
                }
                if (!isGroupRegistered!!) {
                    isGroupRegistered = signalKeyRepository.registerSenderKeyToGroup(groupId, getClientId())
                    roomRepository.remarkJoinInRoom(groupId)
                }

                if (isGroupRegistered!!) {
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

    fun requestCall(groupId: Long) {
        viewModelScope.launch {
            _requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val group = roomRepository.createGroupFromAPI(
                        getClientId(),
                        "",
                        listOf(getClientId(), friendId!!),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.id
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                val success = videoCallRepository.requestVideoCall(groupId)
                _requestCallState.value = if (success) Resource.success(_group.value) else Resource.error("error", _group.value)
            } else {
                _requestCallState.value = Resource.error("error", _group.value)
            }
        }
    }
}
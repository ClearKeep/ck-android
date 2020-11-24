package com.clearkeep.screen.chat.room

import androidx.lifecycle.*
import com.clearkeep.db.model.GROUP_ID_TEMPO
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.screen.chat.repositories.ChatRepository
import com.clearkeep.screen.chat.repositories.GroupRepository
import com.clearkeep.screen.chat.repositories.SignalKeyRepository
import com.clearkeep.db.model.Message
import com.clearkeep.screen.chat.main.people.PeopleRepository
import com.clearkeep.screen.chat.repositories.MessageRepository
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import javax.inject.Inject

class RoomViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: GroupRepository,
        private val groupRepository: GroupRepository,
        private val signalKeyRepository: SignalKeyRepository,
        private val userManager: UserManager,
        private val peopleRepository: PeopleRepository,
        private val messageRepository: MessageRepository
): ViewModel() {
    private val _group = MutableLiveData<ChatGroup>()

    val group: LiveData<ChatGroup>
        get() = _group

    fun getMessages(groupId: String): LiveData<List<Message>> {
        return messageRepository.getMessages(groupId)
    }

    fun getClientId() = chatRepository.getClientId()

    fun getUserName() = userManager.getUserName()

    fun updateGroupWithId(groupId: String) {
        viewModelScope.launch {
            _group.value = roomRepository.getGroupByID(groupId)
            messageRepository.fetchMessageToStore(groupId)
        }
    }

    fun updateGroupWithFriendId(friendId: String) {
        viewModelScope.launch {
            var existingGroup = roomRepository.getGroupPeerByClientId(listOf(getClientId(), friendId))
            if (existingGroup == null) {
                val friend = peopleRepository.getFriend(friendId)
                existingGroup = roomRepository.getTemporaryGroupWithAFriend(getClientId(), getUserName(),
                        friendId, friend.userName)
            } else {
                messageRepository.fetchMessageToStore(existingGroup.id)
            }
            _group.value = existingGroup
        }
    }

    fun sendMessageToUser(receiverId: String, receiverUserName: String, groupId: String, message: String) {
        if (receiverId.isNullOrEmpty()) {
            printlnCK("sendMessageToUser: can not send message with receiverId empty")
            return
        }

        viewModelScope.launch {
            var lastGroupId: String = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val group = roomRepository.createGroupFromAPI(
                        getClientId(),
                        "${getUserName()},$receiverUserName",
                        listOf(getClientId(), receiverId),
                        false
                )
                if (group != null) {
                    _group.value = group
                    lastGroupId = group.id
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                chatRepository.sendMessageInPeer(receiverId, lastGroupId, message)
            }
        }
    }

    fun sendMessageToGroup(groupId: String, message: String, isRegisteredGroup: Boolean) {
        viewModelScope.launch {
            if (!isRegisteredGroup) {
                signalKeyRepository.registerSenderKeyToGroup(groupId, getClientId())
            }
            chatRepository.sendMessageToGroup(groupId, message)
        }
    }

    fun inviteToGroup(invitedFriendId: String, groupId: String) {
        viewModelScope.launch {
            groupRepository.inviteToGroupFromAPI(getClientId(),invitedFriendId, groupId)
        }
    }
}

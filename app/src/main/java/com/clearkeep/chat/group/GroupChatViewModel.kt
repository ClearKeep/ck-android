package com.clearkeep.chat.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import com.clearkeep.chat.repositories.SignalKeyRepository
import com.clearkeep.db.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class GroupChatViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: RoomRepository,
        private val signalKeyRepository: SignalKeyRepository
): ViewModel() {
    fun getClientId() = chatRepository.getClientId()

    fun getGroupRoomList() = roomRepository.getGroupRooms()

    fun getMessagesInRoom(roomId: Int): LiveData<List<Message>> {
        return chatRepository.getMessagesFromRoom(roomId)
    }

    fun getRoom(roomId: Int) = roomRepository.getRoom(roomId)

    fun sendMessage(roomId: Int, groupId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessageToGroup(roomId, groupId, message)
        }
    }

    fun createNewGroup(groupName: String, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.insertGroupRoom(groupName, groupId)
        }
    }

    fun joinInGroup(roomId: Int, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            signalKeyRepository.joinInGroup(roomId, groupId)
        }
    }
}

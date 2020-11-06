package com.clearkeep.chat.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.GroupChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import com.clearkeep.db.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class GroupChatViewModel @Inject constructor(
    private val chatRepository: GroupChatRepository,
    private val roomRepository: RoomRepository
): ViewModel() {

    fun getMyClientId() = chatRepository.getMyClientId()

    fun getMessageList(roomId: Int): LiveData<List<Message>> {
        return chatRepository.getMessagesFromRoom(roomId)
    }

    fun sendMessage(roomId: Int, groupId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(roomId, groupId, message)
        }
    }

    fun insertGroupRoom(groupName: String, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.insertGroupRoom(groupName, groupId)
        }
    }

    fun getGroupRooms() = roomRepository.getGroupRooms()
}

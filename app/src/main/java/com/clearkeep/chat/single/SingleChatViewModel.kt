package com.clearkeep.chat.single

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repo.ChatRepository
import com.clearkeep.chat.repo.RoomRepository
import com.clearkeep.db.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SingleChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val roomRepository: RoomRepository
): ViewModel() {

    fun getMyName() = chatRepository.getMyName()

    fun getMessageList(senderId: String): LiveData<List<Message>> {
        return chatRepository.getMessagesFromSender(senderId)
    }

    fun sendMessage(receiverId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(receiverId, message)
        }
    }

    fun insertSingleRoom(remoteId: String) {
        roomRepository.insertSingleRoom(remoteId)
    }

    fun getSingleRooms() = roomRepository.getSingleRooms()
}

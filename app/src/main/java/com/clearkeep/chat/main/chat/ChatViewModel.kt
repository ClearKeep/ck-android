package com.clearkeep.chat.main.chat

import androidx.lifecycle.ViewModel
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import javax.inject.Inject

class ChatViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: RoomRepository,
): ViewModel() {
    fun getClientId() = chatRepository.getClientId()

    fun getChatHistoryList() = roomRepository.getAllRooms()
}

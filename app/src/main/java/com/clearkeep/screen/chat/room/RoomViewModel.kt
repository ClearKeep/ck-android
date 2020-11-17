package com.clearkeep.screen.chat.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.screen.chat.repositories.ChatRepository
import com.clearkeep.screen.chat.repositories.RoomRepository
import com.clearkeep.screen.chat.repositories.SignalKeyRepository
import com.clearkeep.db.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class RoomViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: RoomRepository,
        private val signalKeyRepository: SignalKeyRepository
): ViewModel() {
    fun getClientId() = chatRepository.getClientId()

    fun getMessagesInRoom(roomId: Int): LiveData<List<Message>> {
        return chatRepository.getMessagesFromRoom(roomId)
    }

    fun getMessagesWithFriend(remoteId: String): LiveData<List<Message>> {
        return chatRepository.getMessagesFromAFriend(remoteId)
    }

    fun sendMessageToUser(remoteId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessageInPeer(remoteId, message)
        }
    }

    fun sendMessageToGroup(roomId: Int, remoteId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isRegisteredGroup = roomRepository.getRoom(roomId)?.isAccepted
            if (!isRegisteredGroup) {
                signalKeyRepository.registerSenderKeyToGroup(roomId, remoteId, getClientId())
            }
            chatRepository.sendMessageToGroup(remoteId, message)
        }
    }
}

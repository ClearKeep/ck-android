package com.clearkeep.chat.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import com.clearkeep.chat.repositories.SignalKeyRepository
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.Room
import com.clearkeep.utilities.printlnCK
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

    suspend fun getRoom(roomId: Int) = roomRepository.getRoom(roomId)

    fun sendMessage(isGroup: Boolean, remoteId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isGroup) {
                chatRepository.sendMessageInPeer(remoteId, message)
            } else {
                chatRepository.sendMessageToGroup(remoteId, message)
            }
        }
    }

    fun createNewGroup(groupName: String, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            /*roomRepository.insertGroupRoom(groupName, groupId)*/
        }
    }

    fun joinInGroup(roomId: Int, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            signalKeyRepository.joinInGroup(roomId, groupId, getClientId())
        }
    }
}

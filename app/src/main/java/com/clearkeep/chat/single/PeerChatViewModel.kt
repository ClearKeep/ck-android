package com.clearkeep.chat.single

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import com.clearkeep.chat.repositories.SignalKeyRepository
import com.clearkeep.db.model.Message
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PeerChatViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: RoomRepository,
        private val signalKeyRepository: SignalKeyRepository,
): ViewModel() {

    init {
        if (!signalKeyRepository.isPeerKeyRegistered()) {
            viewModelScope.launch(Dispatchers.IO) {
                signalKeyRepository.peerRegisterClientKey()
            }
        }
    }

    fun getMyClientId() = chatRepository.getClientId()

    fun getMessageList(roomId: Int): LiveData<List<Message>> {
        return chatRepository.getMessagesFromRoom(roomId)
    }

    fun getRooms() = roomRepository.getPeerRooms()

    fun sendMessage(roomId: Int, receiverId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!signalKeyRepository.isPeerKeyRegistered()) {
                printlnCK("send message is failed because not registered yet")
                signalKeyRepository.peerRegisterClientKey()
            }
            chatRepository.sendMessageInPeer(roomId, receiverId, message)
        }
    }

    fun insertSingleRoom(remoteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.insertPeerRoom(remoteId)
        }
    }
}

package com.clearkeep.screen.chat.main.chat_history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.repo.GroupRepository
import com.clearkeep.utilities.UserManager
import javax.inject.Inject

class ChatViewModel @Inject constructor(
        private val roomRepository: GroupRepository,
        private val userManager: UserManager,
): ViewModel() {
    fun getClientId() = userManager.getClientId()

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()
}

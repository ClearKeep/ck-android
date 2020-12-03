package com.clearkeep.screen.chat.home.chat_history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.utilities.network.Resource
import com.clearkeep.screen.repo.GroupRepository
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ChatViewModel @Inject constructor(
        private val roomRepository: GroupRepository,
        private val userManager: UserManager,
): ViewModel() {
    fun getClientId() = userManager.getClientId()

    fun getClientName() = userManager.getUserName()

    val groups: LiveData<Resource<List<ChatGroup>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emitSource( roomRepository.getAllRooms())
    }
}

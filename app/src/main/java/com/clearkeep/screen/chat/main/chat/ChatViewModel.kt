package com.clearkeep.screen.chat.main.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.repositories.GroupRepository
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ChatViewModel @Inject constructor(
        private val roomRepository: GroupRepository,
        private val profileRepository: ProfileRepository,
        private val userManager: UserManager
): ViewModel() {
    fun getClientId() = profileRepository.getClientId()

    fun getClientName() = userManager.getUserName()

    val groups: LiveData<Resource<List<ChatGroup>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emitSource( roomRepository.getAllRooms())
    }
}

package com.clearkeep.screen.chat.main.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.model.User
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.screen.chat.repositories.ChatRepository
import com.clearkeep.screen.chat.repositories.GroupRepository
import kotlinx.coroutines.Dispatchers.IO
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val roomRepository: GroupRepository,
        private val profileRepository: ProfileRepository
): ViewModel() {

    val profile: LiveData<User> = liveData(viewModelScope.coroutineContext + IO) {
        emit(profileRepository.getProfile())
    }
}

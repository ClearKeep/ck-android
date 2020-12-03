package com.clearkeep.screen.chat.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.repo.ProfileRepository
import kotlinx.coroutines.Dispatchers.IO
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
        private val profileRepository: ProfileRepository
): ViewModel() {

    val profile: LiveData<User?> = liveData(viewModelScope.coroutineContext + IO) {
        emit(profileRepository.getProfile() ?: null)
    }
}

package com.clearkeep.screen.chat.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import kotlinx.coroutines.Dispatchers.IO
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val environment: Environment,
): ViewModel() {

    val profile: LiveData<Profile?> = liveData(viewModelScope.coroutineContext + IO) {
        emit(environment.getServer().profile)
    }

    fun getProfileLink() : String {
        val server = environment.getServer()
        return getLinkFromPeople(User(userId = server.profile.userId, userName = server.profile.getDisplayName(), ownerDomain = server.serverDomain))
    }
}

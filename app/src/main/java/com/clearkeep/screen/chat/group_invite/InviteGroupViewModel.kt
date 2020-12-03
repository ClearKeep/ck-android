package com.clearkeep.screen.chat.group_invite

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.network.Resource
import com.clearkeep.screen.repo.PeopleRepository
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class InviteGroupViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userManager: UserManager
): ViewModel() {
        fun getClientId() = userManager.getClientId()

        val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emitSource(peopleRepository.getFriends(getClientId()))
        }

        fun inviteFriends(friends: List<People>) {
                //
        }
}
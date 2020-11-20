package com.clearkeep.screen.chat.invite_group

import androidx.lifecycle.*
import com.clearkeep.db.model.People
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.main.people.PeopleRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class InviteGroupViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val userRepository: ProfileRepository,
): ViewModel() {
        fun getClientId() = userRepository.getClientId()

        val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emitSource(peopleRepository.getFriends(getClientId()))
        }
}
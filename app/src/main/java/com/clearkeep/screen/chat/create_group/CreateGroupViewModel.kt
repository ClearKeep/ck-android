package com.clearkeep.screen.chat.create_group

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.model.People
import com.clearkeep.repository.UserRepository
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.main.people.PeopleRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class CreateGroupViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userRepository: UserRepository
): ViewModel() {
        fun getClientId() = userRepository.getUserName()

        val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emitSource(peopleRepository.getFriends(getClientId()))
        }
}

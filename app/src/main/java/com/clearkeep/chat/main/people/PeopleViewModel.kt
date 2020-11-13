package com.clearkeep.chat.main.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.PeopleRepository
import com.clearkeep.db.model.People
import kotlinx.coroutines.launch
import javax.inject.Inject

class PeopleViewModel @Inject constructor(
        private val chatRepository: ChatRepository,
        private val peopleRepository: PeopleRepository
): ViewModel() {
    fun getClientId() = chatRepository.getClientId()

    fun getFriendList() = peopleRepository.getFriends()

    fun addFriend(people: People) {
        viewModelScope.launch {
            peopleRepository.addFriend(people)
        }
    }
}

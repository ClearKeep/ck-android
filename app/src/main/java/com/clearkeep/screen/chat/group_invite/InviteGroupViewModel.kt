package com.clearkeep.screen.chat.group_invite

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.screen.chat.utils.getPeopleFromLink
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import javax.inject.Inject

class InviteGroupViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userManager: UserManager
): ViewModel() {
        fun getClientId() = userManager.getClientId()

        private var textSearch = MutableLiveData<String>()

        val friends: LiveData<List<People>> = peopleRepository.getFriends()

        val filterFriends = liveData<List<People>> {
                val result = MediatorLiveData<List<People>>()
                result.addSource(friends) { friendList ->
                        result.value = getFilterFriends(friendList ?: emptyList(), textSearch.value ?: "")
                }
                result.addSource(textSearch) { text ->
                        result.value = getFilterFriends(friends.value ?: emptyList(), text)
                }
                emitSource(result)
        }

        private fun getFilterFriends(list: List<People>, search: String): List<People> {
                return list.filter { search.isBlank() || it.userName.toLowerCase().contains(search) }
        }

        fun insertFriend(people: People) {
                viewModelScope.launch {
                        peopleRepository.insertFriend(people)
                }
        }

        fun search(text: String) {
                textSearch.value = text.trim().toLowerCase()
        }

        fun updateContactList() {
                printlnCK("update contact list from remote API")
                viewModelScope.launch {
                        peopleRepository.updatePeople()
                }
        }
}
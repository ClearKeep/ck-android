package com.clearkeep.screen.chat.group_invite

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.PeopleRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import javax.inject.Inject

class InviteGroupViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val environment: Environment
): ViewModel() {
        fun getClientId() = environment.getServer().profile.userId

        private var textSearch = MutableLiveData<String>()

        val friends: LiveData<List<User>> = peopleRepository.getFriends()

        val filterFriends = liveData<List<User>> {
                val result = MediatorLiveData<List<User>>()
                result.addSource(friends) { friendList ->
                        result.value = getFilterFriends(friendList ?: emptyList(), textSearch.value ?: "")
                }
                result.addSource(textSearch) { text ->
                        result.value = getFilterFriends(friends.value ?: emptyList(), text)
                }
                emitSource(result)
        }

        private fun getFilterFriends(list: List<User>, search: String): List<User> {
                return list.filter { search.isBlank() || it.userName.toLowerCase().contains(search) }
        }

        fun insertFriend(people: User) {
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
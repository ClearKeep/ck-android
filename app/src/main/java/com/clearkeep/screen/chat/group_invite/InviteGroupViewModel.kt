package com.clearkeep.screen.chat.group_invite

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class InviteGroupViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val environment: Environment
) : ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    private var textSearch = MutableLiveData<String>()

    val friends: LiveData<List<User>> = peopleRepository.getFriends(getDomain(), getClientId())

    val checkUserUrlResponse = MutableLiveData<Resource<User>>()

    private var checkUserUrlJob: Job? = null

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    val filterFriends = liveData<List<User>> {
        val result = MediatorLiveData<List<User>>()
        result.addSource(friends) { _ ->
        }
        result.addSource(textSearch) { text ->
            result.value = getFilterFriends(friends.value ?: emptyList(), text)
        }
        emitSource(result)
    }

    private fun getFilterFriends(list: List<User>, search: String): List<User> {
        if (search.isEmpty()) return emptyList()
        return list.filter { search.isBlank() || it.userName.toLowerCase().contains(search) }
    }

    fun insertFriend(people: User) {
        viewModelScope.launch {
            _isLoading.value = true
            peopleRepository.insertFriend(people, owner = getOwner())
            _isLoading.value = false
        }
    }

    private fun getOwner(): Owner {
        return Owner(getDomain(), getClientId())
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

    fun checkUserUrlValid(userId: String, userDomain: String) {
        checkUserUrlJob?.cancel()

        checkUserUrlJob = viewModelScope.launch {
            _isLoading.value = true
            checkUserUrlResponse.value = peopleRepository.getUserInfo(userId, userDomain)
            _isLoading.value = false
        }
    }
}
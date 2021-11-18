package com.clearkeep.presentation.screen.chat.groupinvite

import androidx.lifecycle.*
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.repository.PeopleRepository
import com.clearkeep.domain.usecase.people.GetFriendsUseCase
import com.clearkeep.domain.usecase.people.GetUserInfoUseCase
import com.clearkeep.domain.usecase.people.InsertFriendUseCase
import com.clearkeep.domain.usecase.people.UpdatePeopleUseCase
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteGroupViewModel @Inject constructor(
    private val environment: Environment,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val insertFriendUseCase: InsertFriendUseCase,
    private val updatePeopleUseCase: UpdatePeopleUseCase,
    getFriendsUseCase: GetFriendsUseCase
) : ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    private fun getDomain() = environment.getServer().serverDomain

    private var textSearch = MutableLiveData<String>()

    val friends: LiveData<List<User>> = getFriendsUseCase(getDomain(), getClientId())

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
            insertFriendUseCase(people, owner = getOwner())
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
            updatePeopleUseCase()
        }
    }

    fun checkUserUrlValid(userId: String, userDomain: String) {
        checkUserUrlJob?.cancel()

        checkUserUrlJob = viewModelScope.launch {
            _isLoading.value = true
            checkUserUrlResponse.value = getUserInfoUseCase(userId, userDomain)
            _isLoading.value = false
        }
    }
}
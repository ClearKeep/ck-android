package com.clearkeep.screen.chat.contact_search

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.screen.chat.repo.PeopleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val roomRepository: GroupRepository,
    ) : ViewModel() {
    private val debouncePeriod: Long = 1000

    private var searchJob: Job? = null

    private val _friends: MutableLiveData<List<User>> = MutableLiveData()
    val isShowLoading:MutableLiveData<Boolean> = MutableLiveData()

    val friends: LiveData<List<User>> get() = _friends

    init {
        search("")
    }

    fun search(text: String) {
        isShowLoading.postValue(true)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(debouncePeriod)
            _friends.value = peopleRepository.searchUser(text)
            isShowLoading.postValue(false)
        }
    }
}

enum class StatusRequest(){
    REQUESTING,
    DONE
}
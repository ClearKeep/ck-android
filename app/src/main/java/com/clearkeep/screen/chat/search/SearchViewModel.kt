package com.clearkeep.screen.chat.search

import androidx.lifecycle.*
import com.clearkeep.db.model.People
import com.clearkeep.screen.chat.main.people.PeopleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
) : ViewModel() {
    var debouncePeriod: Long = 1000

    private var searchJob: Job? = null

    private val _friends: MutableLiveData<List<People>> = MutableLiveData()

    val friends: LiveData<List<People>> get() = _friends

    fun search(text: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(debouncePeriod)
            _friends.value = peopleRepository.searchUser(text)
        }
    }
}
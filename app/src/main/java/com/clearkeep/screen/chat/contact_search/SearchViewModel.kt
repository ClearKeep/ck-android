package com.clearkeep.screen.chat.contact_search

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.PeopleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
) : ViewModel() {
    private val debouncePeriod: Long = 1000

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
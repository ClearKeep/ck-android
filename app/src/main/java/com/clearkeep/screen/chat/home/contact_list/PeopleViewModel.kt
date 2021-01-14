package com.clearkeep.screen.chat.home.contact_list

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.repo.PeopleRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PeopleViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userManager: UserManager
): ViewModel() {
    fun getClientId() = userManager.getClientId()

    val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emitSource(peopleRepository.getFriends(getClientId()))
    }

    fun updatePeople() {
        printlnCK("get people")
        viewModelScope.launch {
            peopleRepository.updatePeople()
        }
    }
}

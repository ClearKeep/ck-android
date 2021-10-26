package com.clearkeep.screen.chat.contact_list

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import javax.inject.Inject

class PeopleViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,

    private val environment: Environment
): ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    val friends: LiveData<List<User>> = peopleRepository.getFriends(environment.getServer().serverDomain, getClientId())

    fun updateContactList() {
        printlnCK("update contact list from remote API")
        viewModelScope.launch {
            peopleRepository.updatePeople()
        }
    }
}

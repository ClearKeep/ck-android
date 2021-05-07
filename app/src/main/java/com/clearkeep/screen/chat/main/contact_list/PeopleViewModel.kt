package com.clearkeep.screen.chat.main.contact_list

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch
import javax.inject.Inject

class PeopleViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userManager: UserManager
): ViewModel() {
    fun getClientId() = userManager.getClientId()

    val friends: LiveData<List<People>> = peopleRepository.getFriends()

    fun updateContactList() {
        printlnCK("update contact list from remote API")
        viewModelScope.launch {
            peopleRepository.updatePeople()
        }
    }
}

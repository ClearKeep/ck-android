package com.clearkeep.presentation.screen.chat.contact_list

import androidx.lifecycle.*
import com.clearkeep.domain.model.User
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.repository.PeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    peopleRepository: PeopleRepository,
    private val environment: Environment
) : ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    val friends: LiveData<List<User>> =
        peopleRepository.getFriends(environment.getServer().serverDomain, getClientId())
}

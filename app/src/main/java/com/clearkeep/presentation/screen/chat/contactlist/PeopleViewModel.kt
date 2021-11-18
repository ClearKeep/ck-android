package com.clearkeep.presentation.screen.chat.contactlist

import androidx.lifecycle.*
import com.clearkeep.domain.model.User
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.repository.PeopleRepository
import com.clearkeep.domain.usecase.people.GetFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val environment: Environment
) : ViewModel() {
    fun getClientId() = environment.getServer().profile.userId

    val friends: LiveData<List<User>> =
        getFriendsUseCase(environment.getServer().serverDomain, getClientId())
}

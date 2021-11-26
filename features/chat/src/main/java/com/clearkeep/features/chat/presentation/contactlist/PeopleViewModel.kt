package com.clearkeep.features.chat.presentation.contactlist

import androidx.lifecycle.*
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.people.GetFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    getFriendsUseCase: GetFriendsUseCase,
    private val environment: Environment
) : ViewModel() {
    private fun getClientId() = environment.getServer().profile.userId

    val friends: LiveData<List<User>> =
        getFriendsUseCase(environment.getServer().serverDomain, getClientId())
}

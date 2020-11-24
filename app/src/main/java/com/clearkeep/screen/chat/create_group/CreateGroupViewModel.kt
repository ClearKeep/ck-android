package com.clearkeep.screen.chat.create_group

import androidx.lifecycle.*
import com.clearkeep.db.model.People
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.main.people.PeopleRepository
import com.clearkeep.screen.chat.repositories.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateGroupViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val userRepository: ProfileRepository,
    private val groupRepository: GroupRepository
): ViewModel() {
        private val invitedFriends: MutableList<People> = mutableListOf()

        private val _createGroupState = MutableLiveData<CreateGroupState>()

        val createGroupState: LiveData<CreateGroupState>
                get() = _createGroupState

        val selectedFriends: List<People> = emptyList()

        fun getClientId() = userRepository.getClientId()

        val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emitSource(peopleRepository.getFriends(getClientId()))
        }

        fun setFriendsList(friends: List<People>) {
                invitedFriends.clear()
                invitedFriends.addAll(friends)
        }

        fun createGroup(groupName: String) {
                viewModelScope.launch {
                        _createGroupState.value = CreateGroupProcessing
                        val invitedFriendsAsString = invitedFriends.map { it.id }
                        if (groupRepository.createGroupFromAPI(getClientId(), groupName, invitedFriendsAsString, true) != null) {
                                _createGroupState.value = CreateGroupSuccess
                        } else {
                                _createGroupState.value = CreateGroupError
                        }
                }
        }
}

sealed class CreateGroupState
object CreateGroupSuccess : CreateGroupState()
object CreateGroupError : CreateGroupState()
object CreateGroupProcessing : CreateGroupState()

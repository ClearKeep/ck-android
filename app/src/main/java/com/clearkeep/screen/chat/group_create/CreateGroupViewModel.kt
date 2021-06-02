package com.clearkeep.screen.chat.group_create

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.GroupRepository
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateGroupViewModel @Inject constructor(
        private val userManager: UserManager,
        private val groupRepository: GroupRepository
): ViewModel() {
        var groupId: Long = -1

        val invitedFriends: MutableList<People> = mutableListOf()

        private val _createGroupState = MutableLiveData<CreateGroupState>()

        val createGroupState: LiveData<CreateGroupState>
                get() = _createGroupState

        fun getClientId() = userManager.getClientId()

        fun setFriendsList(friends: List<People>) {
                invitedFriends.clear()
                invitedFriends.addAll(friends)
        }

        fun createGroup(groupName: String) {
                viewModelScope.launch {
                        _createGroupState.value = CreateGroupProcessing
                        val res = groupRepository.createGroupFromAPI(getClientId(), groupName, invitedFriends, true)
                        if (res != null) {
                                groupId = res.id
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

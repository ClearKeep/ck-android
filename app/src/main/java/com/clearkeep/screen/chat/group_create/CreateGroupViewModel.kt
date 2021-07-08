package com.clearkeep.screen.chat.group_create

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.GroupRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateGroupViewModel @Inject constructor(
        private val groupRepository: GroupRepository,

        private val environment: Environment
): ViewModel() {
        var groupId: Long = -1

        val invitedFriends: MutableList<User> = mutableListOf()

        private val _createGroupState = MutableLiveData<CreateGroupState>()

        val createGroupState: LiveData<CreateGroupState>
                get() = _createGroupState

        fun setFriendsList(friends: List<User>) {
                invitedFriends.clear()
                invitedFriends.addAll(friends)
        }

        fun createGroup(groupName: String) {
                viewModelScope.launch {
                        _createGroupState.value = CreateGroupProcessing
                        val server = environment.getServer()
                        // clone invited list and add me to list
                        val list = mutableListOf<User>()
                        list.addAll(invitedFriends)
                        list.add(User(userId = server.profile.userId, userName = server.profile.getDisplayName(), ownerDomain = server.serverDomain))
                        val res = groupRepository.createGroupFromAPI(server.profile.userId, groupName, list, true)
                        if (res != null) {
                                groupId = res.groupId
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

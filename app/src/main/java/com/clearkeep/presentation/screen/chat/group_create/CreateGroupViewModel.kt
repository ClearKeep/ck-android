package com.clearkeep.presentation.screen.chat.group_create

import androidx.lifecycle.*
import com.clearkeep.domain.model.User
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,

    private val environment: Environment
) : ViewModel() {
    var groupId: Long = -1

    val invitedFriends: MutableList<User> = mutableListOf()

    private val _createGroupState = MutableLiveData<CreateGroupState>()

    val createGroupState: LiveData<CreateGroupState>
        get() = _createGroupState

    fun setFriendsList(friends: List<User>) {
        invitedFriends.clear()
        invitedFriends.addAll(friends)
    }

    fun createGroup(groupName: String, onError: (() -> Unit)? = null) {
        viewModelScope.launch {
            _createGroupState.value = CreateGroupProcessing
            val server = environment.getServer()
            // clone invited list and add me to list
            val list = mutableListOf<User>()
            list.addAll(invitedFriends)
            list.add(
                User(
                    userId = server.profile.userId,
                    userName = server.profile.userName ?: "",
                    domain = server.serverDomain,
                    server.profile.userName,
                    phoneNumber = server.profile.phoneNumber,
                    avatar = server.profile.avatar,
                    email = server.profile.email
                )
            )
            val res =
                groupRepository.createGroup(server.profile.userId, groupName, list, true)
            if (res?.data != null) {
                groupId = res.data.groupId
                _createGroupState.value = CreateGroupSuccess
            } else {
                onError?.invoke()
                _createGroupState.value = CreateGroupError
            }
        }
    }
}

sealed class CreateGroupState
object CreateGroupSuccess : CreateGroupState()
object CreateGroupError : CreateGroupState()
object CreateGroupProcessing : CreateGroupState()

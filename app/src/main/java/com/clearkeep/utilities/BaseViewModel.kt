package com.clearkeep.utilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.repo.GroupRepository
import com.clearkeep.repo.MessageRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

open class BaseViewModel @Inject constructor(
    protected val authRepository: AuthRepository,
    protected val roomRepository: GroupRepository,
    protected val serverRepository: ServerRepository,
    protected val messageRepository: MessageRepository
) : ViewModel() {
    val currentServer = serverRepository.activeServer

    val servers: LiveData<List<Server>> = serverRepository.getServersAsState()

    private val _isLogOutCompleted = MutableLiveData(false)
    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    fun signOut() {
        viewModelScope.launch {
            currentServer.value?.let {
                authRepository.logoutFromAPI(it)
                val removeResult = it.id?.let { it1 -> serverRepository.deleteServer(it1) }
                roomRepository.removeGroupByDomain(
                    it.serverDomain,
                    it.ownerClientId
                )
                messageRepository.clearMessageByDomain(
                    it.serverDomain,
                    it.ownerClientId
                )
                if (removeResult != null && removeResult > 0) {
                    printlnCK("serverRepository: ${serverRepository.getServers().size}")
                    if (serverRepository.getServers().isNotEmpty()) {
                        selectChannel(servers.value!![0])
                    } else {
                        _isLogOutCompleted.value = true
                    }
                }
            }
        }
    }

    open fun selectChannel(server: Server) {
        viewModelScope.launch {
            serverRepository.setActiveServer(server)
        }
    }
}
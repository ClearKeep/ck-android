package com.clearkeep.utilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.screen.chat.repo.GroupRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

open class BaseViewModel @Inject constructor(
    protected val authRepository: AuthRepository,
    protected val roomRepository: GroupRepository,
    protected val serverRepository: ServerRepository,
) : ViewModel() {
    val currentServer = serverRepository.activeServer

    val servers: LiveData<List<Server>> = serverRepository.getServersAsState()

    private val _isLogOutCompleted = MutableLiveData(false)
    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    fun signOut() {
        viewModelScope.launch {
            val response = authRepository.logoutFromAPI(currentServer.value!!)

            if (response.data?.error.isNullOrBlank()) {
                currentServer.value?.id?.let {
                    val removeResult = serverRepository.deleteServer(it)
                    roomRepository.removeGroupByDomain(currentServer.value!!.serverDomain, currentServer.value!!.ownerClientId)
                    if (removeResult > 0) {
                        printlnCK("serverRepository: ${serverRepository.getServers().size}")
                        if (serverRepository.getServers().isNotEmpty()) {
                            printlnCK("servers.value!![0]: ${servers.value!![0]}")
                            selectChannel(servers.value!![0])
                        } else {
                            _isLogOutCompleted.value = true
                        }
                    }
                }
            } else {
                printlnCK("signOut error")
            }
        }
    }

    protected fun selectChannel(server: Server) {
        viewModelScope.launch {
            serverRepository.setActiveServer(server)
//            selectingJoinServer.value = false
        }
    }
}
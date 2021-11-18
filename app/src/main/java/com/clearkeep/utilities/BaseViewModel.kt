package com.clearkeep.utilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.AuthRepository
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.group.DeleteGroupUseCase
import com.clearkeep.domain.usecase.message.DeleteMessageUseCase
import com.clearkeep.domain.usecase.server.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor(
    protected val deleteGroupUseCase: DeleteGroupUseCase,
    protected val deleteMessageUseCase: DeleteMessageUseCase,
    protected val logoutUseCase: LogoutUseCase,
    protected val deleteServerUseCase: DeleteServerUseCase,
    protected val setActiveServerUseCase: SetActiveServerUseCase,
    protected val getServersUseCase: GetServersUseCase,
    getServersAsStateUseCase: GetServersAsStateUseCase,
    getActiveServerUseCase: GetActiveServerUseCase,
) : ViewModel() {
    val currentServer = getActiveServerUseCase()

    val servers: LiveData<List<Server>> = getServersAsStateUseCase()

    private val _isLogOutCompleted = MutableLiveData(false)
    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    fun signOut() {
        viewModelScope.launch {
            val response = logoutUseCase(currentServer.value!!)

            if (response.data?.error.isNullOrBlank()) {
                currentServer.value?.id?.let {
                    val removeResult = deleteServerUseCase(it)
                    deleteGroupUseCase(
                        currentServer.value!!.serverDomain,
                        currentServer.value!!.ownerClientId
                    )
                    deleteMessageUseCase(
                        currentServer.value!!.serverDomain,
                        currentServer.value!!.ownerClientId
                    )
                    if (removeResult > 0) {
                        if (getServersUseCase().isNotEmpty()) {
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

    open fun selectChannel(server: Server) {
        viewModelScope.launch {
            setActiveServerUseCase(server)
        }
    }
}
package com.clearkeep.screen.chat.main.home

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.*
import com.clearkeep.utilities.UserManager
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val roomRepository: GroupRepository,
    private val userManager: UserManager,
    ): ViewModel() {
    var selectedServer = MutableLiveData<Server>()

    var myClintId: String =""

    init {
        // TODO: load channel and select first channel as default
        myClintId = userManager.getClientId()
        selectedServer.value = Server(1, "CK Development", getServerDomain(), "")
    }

    val showJoinServer = MutableLiveData(false)

    val servers: LiveData<List<Server>> = liveData {
        emit(listOf(
            Server(1, "CK Development", getServerDomain(), ""),
            Server(2, "PVN", getServerDomain(), ""),
            Server(3, "VMO", getServerDomain(), "")
        ))
    }

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val chatGroups: LiveData<List<ChatGroup>> = Transformations.map(groups) { all ->
        all.filter { it.isGroup() }
    }

    val directGroups: LiveData<List<ChatGroup>> = Transformations.map(groups) { all ->
        all.filter { !it.isGroup() }
    }

    fun searchGroup(text: String) {}

    fun selectChannel(server: Server) {
        this.selectedServer.value = server
        showJoinServer.value = false
    }

    fun showJoinServer() {
        showJoinServer.value = true
    }

    private fun getServerDomain(): String {
        return userManager.getWorkspaceDomain()
    }
}

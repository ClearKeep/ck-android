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
    var channelIdLive = MutableLiveData<Long>(0L)
    var myClintId: String =""

    init {
        // TODO: load channel and select first channel as default
        myClintId = userManager.getClientId()
    }

    val showJoinServer = MutableLiveData(false)

    val servers: LiveData<List<Server>> = liveData {
        emit(listOf(
            Server(1, "CK Development", ""),
            Server(2, "PVN", ""),
            Server(3, "VMO", "")
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
        this.channelIdLive.value = server.id
        showJoinServer.value = false
    }

    fun showJoinServer() {
        showJoinServer.value = true
    }
}

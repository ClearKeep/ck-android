package com.clearkeep.screen.chat.home.home

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
    var channelIdLive : Long?=null
    var myClintId: String =""

    init {
        // TODO: load channel and select first channel as default
        selectChannel(1)
        myClintId = userManager.getClientId()
    }


    val servers: LiveData<List<Server>> = liveData {
        emit(listOf(Server(1, "CK Development", "")))
    }

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val chatGroups: LiveData<List<ChatGroup>> = Transformations.map(groups) { all ->
        all.filter { it.isGroup() }
    }

    val directGroups: LiveData<List<ChatGroup>> = Transformations.map(groups) { all ->
        all.filter { !it.isGroup() }
    }

    fun searchGroup(text: String) {}

    fun selectChannel(channelId: Long) {
        this.channelIdLive=channelId
    }
}

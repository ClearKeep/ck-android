package com.clearkeep.screen.chat.home.home

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.*
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val roomRepository: GroupRepository,
): ViewModel() {
    init {
        // TODO: load channel and select first channel as default
        selectChannel(1)
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
    val chatGroupDummy: LiveData<List<ChatGroupDummy>> = liveData {
        emit(
            listOf(
            ChatGroupDummy(1,"name dummy 1",1),
            ChatGroupDummy(2,"name dummy 2",6),
            ChatGroupDummy(3,"name dummy 3",4),
            ChatGroupDummy(4,"name dummy 4",10),
            ChatGroupDummy(5,"name dummy 5",12),
            ChatGroupDummy(6,"name dummy 6",13),
        )
        )
    }

    fun searchGroup(text: String) {}

    fun selectChannel(channelId: Long) {}
}
data class ChatGroupDummy(val id:Int,val name:String,val numberUnread:Int)

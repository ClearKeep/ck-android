package com.clearkeep.screen.chat.contact_search

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserStateTypeInGroup
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.screen.chat.repo.PeopleRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val roomRepository: GroupRepository,
    private val environment: Environment,
    serverRepository: ServerRepository,
) : ViewModel() {
    private val debouncePeriod: Long = 1000

    private var searchJob: Job? = null

    private val _friends: MutableLiveData<List<User>> = MutableLiveData()
    val isShowLoading:MutableLiveData<Boolean> = MutableLiveData()
    var profile = serverRepository.getDefaultServerProfileAsState()
    val friends: LiveData<List<User>> get() = _friends

    private val _groups = MediatorLiveData<List<ChatGroup>>()
    val groups : LiveData<List<ChatGroup>> get() = _groups
    private var groupSource: LiveData<List<ChatGroup>> = MutableLiveData()

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery : LiveData<String> get() = _searchQuery

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    fun search(text: String) {
        isShowLoading.postValue(true)
        searchJob?.cancel()
        _searchQuery.value = text
        val server = environment.getServer()
        searchJob = viewModelScope.launch {
            delay(debouncePeriod)
            searchGroups(server, text)
            isShowLoading.postValue(false)
        }
    }

    private suspend fun searchGroups(server: Server, query: String) {
        withContext(Dispatchers.Main) {
            _groups.removeSource(groupSource)
            withContext(Dispatchers.IO) {
                groupSource = roomRepository.getGroupsByGroupName(server.serverDomain, server.profile.userId, query)
            }
            try {
                _groups.addSource(groupSource) {
                    _groups.value = it.filter { it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == UserStateTypeInGroup.ACTIVE.value }
                    printlnCK("group raw $it current profile id ${profile.value?.userId}")
                    printlnCK("group result ${_groups.value}")
                }
            } catch (e: Exception) {
                printlnCK("searchGroups exception $e")
            }
        }
    }
}

enum class StatusRequest(){
    REQUESTING,
    DONE
}
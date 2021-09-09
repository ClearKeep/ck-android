package com.clearkeep.screen.chat.contact_search

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.screen.chat.repo.PeopleRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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

    val isShowLoading:MutableLiveData<Boolean> = MutableLiveData()
    var profile = serverRepository.getDefaultServerProfileAsState()

    private val _friends: MutableLiveData<List<User>> = MutableLiveData()
    val friends: LiveData<List<User>> get() = _friends

    private val _groups = MediatorLiveData<List<ChatGroup>>()
    val groups : LiveData<List<ChatGroup>> get() = _groups
    private var groupSource: LiveData<List<ChatGroup>> = MutableLiveData()

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery : LiveData<String> get() = _searchQuery

    val currentServer = serverRepository.activeServer

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    init {
        viewModelScope.launch {
            peopleRepository.updatePeople()
        }
    }

    fun search(text: String) {
        isShowLoading.postValue(true)
        searchJob?.cancel()
        _searchQuery.value = text
        val server = environment.getServer()
        searchJob = viewModelScope.launch {
            delay(debouncePeriod)
            searchGroups(server, text)
            searchUsers(server, text)
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

    private suspend fun searchUsers(server: Server, query: String) {
        val allPeopleInServer = peopleRepository.getFriends(server.serverDomain, server.profile.userId)
        println("searchUsers allPeopleInServer ${allPeopleInServer.value}")
        allPeopleInServer.asFlow().combine(roomRepository.getPeerRoomsByPeerName(server.serverDomain, server.profile.userId, query).asFlow()) { a: List<User>, b: List<ChatGroup> ->
            println("searchUsers List of chat groups $a list of users $b")
            val usersInSameServer = a.filter { it.userName.contains(query, true) }
            val usersInPeerChat = b.map {
                val userId = it.clientList.find { it.userId != server.profile.userId }?.userId
                User(
                    userId ?: it.ownerClientId,
                    it.groupName,
                    it.ownerDomain,
                    "",
                    "",
                    "",
                    it.groupAvatar,
                    ""
                )
            }
            println("searchUsers usersInSameServer $usersInSameServer usersInPeerChat $usersInPeerChat")
            (usersInSameServer + usersInPeerChat).distinctBy { it.userId }
        }.combine(
            roomRepository.getGroupsByDomain(server.serverDomain, server.profile.userId).asFlow()
        ) { a: List<User>, b: List<ChatGroup> ->
            a + b.map { it.clientList }.flatten().filter { it.userName.contains(query, true) }
        }.collect {
            printlnCK("searchUsers result ${it.distinctBy { it.userId }}")
            _friends.value = it.distinctBy { it.userId }
        }
    }
}

enum class StatusRequest(){
    REQUESTING,
    DONE
}
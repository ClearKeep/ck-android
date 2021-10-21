package com.clearkeep.screen.chat.contact_search

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.screen.chat.repo.MessageRepository
import com.clearkeep.screen.chat.repo.PeopleRepository
import com.clearkeep.utilities.isFileMessage
import com.clearkeep.utilities.isImageMessage
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.lang.Exception
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val messageRepository: MessageRepository,
    private val roomRepository: GroupRepository,
    private val environment: Environment,
    serverRepository: ServerRepository,
) : ViewModel() {
    private var searchJob: Job? = null

    val isShowLoading:MutableLiveData<Boolean> = MutableLiveData()
    var profile = serverRepository.getDefaultServerProfileAsState()

    private val _friends: MutableLiveData<List<User>> = MutableLiveData()
    val friends: LiveData<List<User>> get() = _friends

    private val _groups = MediatorLiveData<List<ChatGroup>>()
    val groups : LiveData<List<ChatGroup>> get() = _groups
    private var groupSource: LiveData<List<ChatGroup>> = MutableLiveData()

    private val _messages = MediatorLiveData<List<MessageSearchResult>>()
    val messages : LiveData<List<MessageSearchResult>> get() = _messages
    private var messagesSource: LiveData<List<MessageSearchResult>> = MutableLiveData()

    private val _searchMode = MutableLiveData<SearchMode>()
    val searchMode : LiveData<SearchMode> get() = _searchMode

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery : LiveData<String> get() = _searchQuery

    val getPeopleResponse = MutableLiveData<Resource<Nothing>>()

    val currentServer = serverRepository.activeServer

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    init {
        viewModelScope.launch {
            getPeopleResponse.value = peopleRepository.updatePeople()
        }
        setSearchMode(SearchMode.ALL)
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode

        _friends.value = null
        _groups.value = null
        _messages.value = null

        searchQuery.value?.let {
            search(it)
        }
    }

    fun search(text: String) {
        isShowLoading.postValue(true)
        searchJob?.cancel()
        if (text.isBlank()) {
            _friends.value = null
            _groups.value = null
            _messages.value = null
            return
        }
        _searchQuery.value = text
        val server = environment.getServer()
        searchJob = viewModelScope.launch {
            when(searchMode.value) {
                SearchMode.ALL -> {
                    searchGroups(server, text)
                    searchMessages(server, text)
                    searchUsers(server, text)
                }
                SearchMode.PEOPLE -> {
                    searchUsers(server, text)
                }
                SearchMode.GROUPS -> {
                    searchGroups(server, text)
                }
                SearchMode.MESSAGES -> {
                    searchMessages(server, text)
                }
            }
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
                    _groups.value = it.filter { it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == UserStateTypeInGroup.ACTIVE.value }.sortedByDescending { it.lastMessageAt }
                    printlnCK("group raw $it current profile id ${profile.value?.userId}")
                    printlnCK("group result ${_groups.value}")
                }
            } catch (e: Exception) {
                printlnCK("searchGroups exception $e")
            }
        }
    }

    private suspend fun searchUsers(server: Server, query: String) {
        val allPeopleInGroupChats =
            roomRepository.getGroupsByDomain(server.serverDomain, server.profile.userId).asFlow()
        val allPeopleInServer =
            peopleRepository.getFriends(server.serverDomain, server.profile.userId)
        val allPeerChat = roomRepository.getPeerRoomsByPeerName(
            server.serverDomain,
            server.profile.userId,
            query
        )

        val allUnchattedPeople = allPeopleInGroupChats.combine(allPeopleInServer.asFlow()) { a: List<ChatGroup>, b: List<User> ->
            val usersFromGroupChatFiltered =
                a.map { it.clientList }.flatten().filter { it.userId != server.profile.userId && it.userName.contains(query, true) }
            val usersInServerFiltered =
                b.filter { it.userName.contains(query, true) && it.userId != server.profile.userId }

            printlnCK("usersInServerFiltered $usersInServerFiltered")

            (usersFromGroupChatFiltered + usersInServerFiltered).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.userName }))
        }

        allPeerChat.asFlow().combine(allUnchattedPeople) { a: List<ChatGroup>, b: List<User> ->
            val usersInPeerChat = a.sortedByDescending { it.lastMessageAt }.map {
                printlnCK("searchUsers private chat group $it")
                val user = it.clientList.find { it.userId != server.profile.userId }
                User(
                    user?.userId ?: it.ownerClientId,
                    user?.userName ?: it.groupName,
                    user?.domain ?: it.ownerDomain,
                    "",
                    "",
                    "",
                    it.groupAvatar,
                    ""
                )
            }
            (usersInPeerChat + b).distinctBy { it.userId }
        }.collect {
            printlnCK("searchUsers result ${it.distinctBy { it.userId }}")
            _friends.value = it.distinctBy { it.userId }
        }
    }

    private suspend fun searchMessages(server: Server, query: String) {
        withContext(Dispatchers.Main) {
            _messages.removeSource(messagesSource)
            withContext(Dispatchers.IO) {
                messagesSource = messageRepository.getMessageByText(server.serverDomain, server.profile.userId, query)
                printlnCK("message result ${messagesSource.value}")
            }
            try {
                _messages.addSource(messagesSource) {
                    _messages.value =
                        it.distinctBy { it.message.messageId }.filterNot { isFileMessage(it.message.message) || isImageMessage(it.message.message) }
                            .sortedByDescending { it.message.createdTime }
                    printlnCK("message result ${_messages.value}")
                }
            } catch (e: Exception) {
                printlnCK("searchGroups exception $e")
            }
        }
    }

    fun insertFriend(people: User) {
        viewModelScope.launch {
            peopleRepository.insertFriend(people, owner = Owner(getDomainOfActiveServer(), getClientIdOfActiveServer()))
        }
    }
}

enum class SearchMode {
    ALL,
    PEOPLE,
    GROUPS,
    MESSAGES
}

data class MessageSearchResult(val message: Message, val user: User?, val group: ChatGroup?)

enum class StatusRequest(){
    REQUESTING,
    DONE
}
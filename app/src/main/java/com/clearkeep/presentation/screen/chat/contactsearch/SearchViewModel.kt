package com.clearkeep.presentation.screen.chat.contactsearch

import androidx.lifecycle.*
import com.clearkeep.common.utilities.isFileMessage
import com.clearkeep.common.utilities.isImageMessage
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.*
import com.clearkeep.domain.usecase.group.GetGroupsByDomainUseCase
import com.clearkeep.domain.usecase.group.GetGroupsByGroupNameUseCase
import com.clearkeep.domain.usecase.group.GetPeerRoomsByPeerNameUseCase
import com.clearkeep.domain.usecase.message.GetMessageByTextUseCase
import com.clearkeep.domain.usecase.people.GetFriendsUseCase
import com.clearkeep.domain.usecase.people.InsertFriendUseCase
import com.clearkeep.domain.usecase.people.UpdatePeopleUseCase
import com.clearkeep.domain.usecase.server.GetActiveServerUseCase
import com.clearkeep.domain.usecase.server.GetDefaultServerProfileAsStateUseCase
import com.clearkeep.common.utilities.printlnCK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val environment: Environment,
    private val getGroupsByGroupNameUseCase: GetGroupsByGroupNameUseCase,
    private val getGroupsByDomainUseCase: GetGroupsByDomainUseCase,
    private val getPeerRoomsByPeerNameUseCase: GetPeerRoomsByPeerNameUseCase,
    private val getMessageByTextUseCase: GetMessageByTextUseCase,
    private val insertFriendUseCase: InsertFriendUseCase,
    private val updatePeopleUseCase: UpdatePeopleUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    getDefaultServerProfileAsStateUseCase: GetDefaultServerProfileAsStateUseCase,
    getActiveServerUseCase: GetActiveServerUseCase,
) : ViewModel() {
    private var searchJob: Job? = null

    val isShowLoading: MutableLiveData<Boolean> = MutableLiveData()
    var profile = getDefaultServerProfileAsStateUseCase()

    private val _friends: MutableLiveData<List<User>> = MutableLiveData()
    val friends: LiveData<List<User>> get() = _friends

    private val _groups = MediatorLiveData<List<ChatGroup>>()
    val groups: LiveData<List<ChatGroup>> get() = _groups
    private var groupSource: LiveData<List<ChatGroup>> = MutableLiveData()

    private val _messages = MediatorLiveData<List<MessageSearchResult>>()
    val messages: LiveData<List<MessageSearchResult>> get() = _messages
    private var messagesSource: LiveData<List<MessageSearchResult>> = MutableLiveData()

    private val _searchMode = MutableLiveData<SearchMode>()
    val searchMode: LiveData<SearchMode> get() = _searchMode

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> get() = _searchQuery

    val getPeopleResponse = MutableLiveData<com.clearkeep.common.utilities.network.Resource<Nothing>>()

    val currentServer = getActiveServerUseCase()

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    init {
        viewModelScope.launch {
            getPeopleResponse.value = updatePeopleUseCase()
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
            when (searchMode.value) {
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
                groupSource = getGroupsByGroupNameUseCase(
                    server.serverDomain,
                    server.profile.userId,
                    query
                )
            }
            try {
                _groups.addSource(groupSource) {
                    _groups.value =
                        it.filter { it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == UserStateTypeInGroup.ACTIVE.value }
                            .sortedByDescending { it.lastMessageAt }
                }
            } catch (e: Exception) {
                printlnCK("searchGroups exception $e")
            }
        }
    }

    private suspend fun searchUsers(server: Server, query: String) {
        val allPeopleInGroupChats =
            getGroupsByDomainUseCase(server.serverDomain, server.profile.userId).asFlow()
        val allPeopleInServer =
            getFriendsUseCase(server.serverDomain, server.profile.userId)
        val allPeerChat = getPeerRoomsByPeerNameUseCase(
            server.serverDomain,
            server.profile.userId,
            query
        )

        val allUnchattedPeople =
            allPeopleInGroupChats.combine(allPeopleInServer.asFlow()) { a: List<ChatGroup>, b: List<User> ->
                val usersFromGroupChatFiltered =
                    a.map { it.clientList }.flatten().filter {
                        it.userId != server.profile.userId && it.userName.contains(
                            query,
                            true
                        )
                    }
                val usersInServerFiltered =
                    b.filter {
                        it.userName.contains(
                            query,
                            true
                        ) && it.userId != server.profile.userId
                    }

                (usersFromGroupChatFiltered + usersInServerFiltered).sortedWith(
                    compareBy(
                        String.CASE_INSENSITIVE_ORDER,
                        { it.userName })
                )
            }

        allPeerChat.asFlow().combine(allUnchattedPeople) { a: List<ChatGroup>, b: List<User> ->
            val usersInPeerChat = a.sortedByDescending { it.lastMessageAt }.map {
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
            _friends.value = it.distinctBy { it.userId }
        }
    }

    private suspend fun searchMessages(server: Server, query: String) {
        withContext(Dispatchers.Main) {
            _messages.removeSource(messagesSource)
            withContext(Dispatchers.IO) {
                messagesSource = getMessageByTextUseCase(
                    server.serverDomain,
                    server.profile.userId,
                    query
                )
            }
            try {
                _messages.addSource(messagesSource) {
                    _messages.value =
                        it.distinctBy { it.message.messageId }
                            .filterNot { isFileMessage(it.message.message) || isImageMessage(it.message.message) }
                            .sortedByDescending { it.message.createdTime }
                }
            } catch (e: Exception) {
                printlnCK("searchGroups exception $e")
            }
        }
    }

    fun insertFriend(people: User) {
        viewModelScope.launch {
            insertFriendUseCase(
                people,
                owner = Owner(
                    getDomainOfActiveServer(),
                    getClientIdOfActiveServer()
                )
            )
        }
    }
}

enum class SearchMode {
    ALL,
    PEOPLE,
    GROUPS,
    MESSAGES
}

enum class StatusRequest() {
    REQUESTING,
    DONE
}
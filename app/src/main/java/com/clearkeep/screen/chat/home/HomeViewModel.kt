package com.clearkeep.screen.chat.home

import androidx.lifecycle.*
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.*
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import com.clearkeep.utilities.FIREBASE_TOKEN
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.UserPreferencesStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val roomRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val profileRepository: ProfileRepository,
    private val messageRepository: MessageRepository,
    private val environment: Environment,

    private val authRepository: AuthRepository,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val storage: UserPreferencesStorage,
    private val clearKeepDatabase: ClearKeepDatabase,
    private val signalKeyDatabase: SignalKeyDatabase,
    private val workSpaceRepository: WorkSpaceRepository,
    private val peopleRepository: PeopleRepository
    ): ViewModel() {

    var profile = serverRepository.getDefaultServerProfileAsState()

    var currentServer = serverRepository.activeServer

    val selectingJoinServer = MutableLiveData(false)
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    val servers: LiveData<List<Server>> = serverRepository.getServersAsState()

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val isRefreshing= MutableLiveData(false)

    private val _currentStatus = MutableLiveData(UserStatus.ONLINE.value)
    val currentStatus: LiveData<String>

    get() = _currentStatus
    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserInfo: LiveData<List<User>>
        get() = _listUserStatus

    init {
        printlnCK("Share file cancel HomeViewModel init")
        viewModelScope.launch {
            messageRepository.clearTempNotes()
            messageRepository.clearTempMessage()
            roomRepository.fetchGroups()
        }
        getStatusUserInDirectGroup()
        sendPing()
    }

    fun onPullToRefresh(){
        isRefreshing.postValue(true)
        viewModelScope.launch {
            roomRepository.fetchGroups()
            isRefreshing.postValue(false)

        }
    }

    val chatGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()
        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == UserStateTypeInGroup.ACTIVE.value

            }
        }

        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
            }
        }
        emitSource(result)
    }

    val directGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()

        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            getStatusUserInDirectGroup()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        emitSource(result)
    }

    private fun getStatusUserInDirectGroup() {
        viewModelScope.launch {
            groups.asFlow().collect {
                val listUserRequest = arrayListOf<User>()
                it.forEach { group ->
                    if (!group.isGroup()) {
                        val user = group.clientList.firstOrNull { client ->
                            client.userId != getClientIdOfActiveServer()
                        }
                        if (user != null) {
                            listUserRequest.add(user)
                        }
                    }
                }
                val listClientStatus = peopleRepository.getListClientStatus(listUserRequest)
                _listUserStatus.postValue(listClientStatus)
                listClientStatus?.forEach {
                    currentServer.value?.serverDomain?.let { it1 ->
                        currentServer.value?.ownerClientId?.let { it2 ->
                            Owner(it1, it2)
                        }
                    }?.let { it2 -> peopleRepository.updateAvatarUserEntity(it,owner = it2) }
                }
            }
        }
    }

    private fun sendPing() {
        viewModelScope.launch {
            delay(30*1000)
            peopleRepository.sendPing()
            sendPing()
        }
    }

    fun setUserStatus(status: UserStatus) {
        viewModelScope.launch {
           val result= peopleRepository.updateStatus(status.value)
            if (result) _currentStatus.postValue(status.value)
        }
    }

    fun prepare() {
        viewModelScope.launch {
            _prepareState.value = PrepareProcessing
            updateFirebaseToken()
            _prepareState.value = PrepareSuccess
        }
    }

    fun searchGroup(text: String) {}

    fun selectChannel(server: Server) {
        viewModelScope.launch {
            serverRepository.setActiveServer(server)
            selectingJoinServer.value = false
        }
    }

    fun showJoinServer() {
        selectingJoinServer.value = true
    }

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    private val _isLogOutProcessing = MutableLiveData<Boolean>()

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private val _isLogOutCompleted = MutableLiveData(false)

    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    private fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                printlnCK("Fetching FCM registration token failed, ${task.exception}")
            }

            // Get new FCM registration token
            val token = task.result
            if (!token.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    storage.setString(FIREBASE_TOKEN, token)
                    pushFireBaseTokenToServer()
                }
            }
        }
    }

    fun leaveServer() {

        viewModelScope.launch {

            /*workSpaceRepository.leaveServer()
            *//*currentServer.value?.id?.let { serverRepository.deleteServer(it) }
            currentServer.value?.id?.let {
              var resutf=  serverRepository.deleteServer(it)
            }
            printlnCK("leaveServer: ${servers.value?.size} ")
            if (servers.value?.size!! > 0) {
                printlnCK("leaveServer: ${servers.value?.size} ")
                selectChannel(servers.value!![0])
            }else {
                logOut()
            }*/
            val result = workSpaceRepository.leaveServer()
            if (result?.success == true){
                currentServer.value?.id?.let {
                    val removeResult = serverRepository.deleteServer(it)
                    roomRepository.removeGroupByDomain(currentServer.value!!.serverDomain, currentServer.value!!.ownerClientId)
                    if (removeResult > 0) {
                        printlnCK("serverRepository: ${serverRepository.getServers().size}")
                        if (serverRepository.getServers().isNotEmpty()) {
                            printlnCK("servers.value!![0]: ${servers.value!![0]}")
                            selectChannel(servers.value!![0])
                        }else {
                            _isLogOutCompleted.value = true
                        }
                    }
                }
            }else {
                printlnCK("Leave Server error")
            }

            /*if (result?.success == true) {
                currentServer.value?.id?.let { serverRepository.deleteServer(it) }
                if (servers.value?.size!! > 0) {
                    selectChannel(servers.value!![0])
                }else {
                    logOut()
                }
            } else {
                printlnCK("leaveServer error")
            }*/
        }
    }

    private suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        storage.clear()
        signalProtocolStore.clear()
        clearKeepDatabase.clearAllTables()
        signalKeyDatabase.clearAllTables()
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        val token = storage.getString(FIREBASE_TOKEN)
        if (!token.isNullOrEmpty()) {
            profileRepository.registerToken(token)
        }
    }

    fun getProfileLink() : String {
        val server = environment.getServer()
        return getLinkFromPeople(User(userId = server.profile.userId, userName = server.profile.userName ?: "", domain = server.serverDomain))
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()

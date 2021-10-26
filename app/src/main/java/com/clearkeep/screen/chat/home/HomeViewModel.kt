package com.clearkeep.screen.chat.home

import androidx.lifecycle.*
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.dao.SignalKeyDAO
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.*
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.storage.UserPreferencesStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import org.whispersystems.libsignal.groups.SenderKeyName
import javax.inject.Inject


class HomeViewModel @Inject constructor(
    roomRepository: GroupRepository,
    serverRepository: ServerRepository,
    private val profileRepository: ProfileRepository,
    messageRepository: MessageRepository,
    private val environment: Environment,
    authRepository: AuthRepository,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val storage: UserPreferencesStorage,
    private val clearKeepDatabase: ClearKeepDatabase,
    private val signalKeyDatabase: SignalKeyDatabase,
    private val workSpaceRepository: WorkSpaceRepository,
    private val peopleRepository: PeopleRepository,
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalIdentityKeyDAO: SignalIdentityKeyDAO,
    private val signalKeyDAO : SignalKeyDAO,
    private val signalPreKeyDAO: SignalPreKeyDAO
    ): BaseViewModel(authRepository, roomRepository, serverRepository, messageRepository) {
    var profile = serverRepository.getDefaultServerProfileAsState()

    val isLogout = serverRepository.isLogout

    val selectingJoinServer = MutableLiveData(false)
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val isRefreshing= MutableLiveData(false)

    private val _currentStatus = MutableLiveData(UserStatus.ONLINE.value)
    val currentStatus: LiveData<String>

    get() = _currentStatus
    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserInfo: LiveData<List<User>>
        get() = _listUserStatus

    val serverUrlValidateResponse = MutableLiveData<String>()

    private val _isServerUrlValidateLoading = MutableLiveData<Boolean>()
    val isServerUrlValidateLoading : LiveData<Boolean>
        get() = _isServerUrlValidateLoading

    private var checkValidServerJob: Job? = null

    init {
        viewModelScope.launch {
            messageRepository.clearTempNotes()
            messageRepository.clearTempMessage()
            roomRepository.fetchGroups()
            getStatusUserInDirectGroup()
        }

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
            //getStatusUserInDirectGroup()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        emitSource(result)
    }

    private suspend fun getStatusUserInDirectGroup() {
        try {
                val listUserRequest = arrayListOf<User>()
                roomRepository.getAllPeerGroupByDomain(
                    owner = Owner(
                        getDomainOfActiveServer(),
                        getClientIdOfActiveServer()
                    )
                )
                    .forEach { group ->
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
                    }?.let { it2 -> peopleRepository.updateAvatarUserEntity(it, owner = it2) }
                }
                delay(60 * 1000)
                getStatusUserInDirectGroup()

        } catch (e: Exception) {
            printlnCK("getStatusUserInDirectGroup error: ${e.message}")
        }
    }

    private fun sendPing() {
        viewModelScope.launch {
            delay(60*1000)
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

    override fun selectChannel(server: Server) {
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

    fun checkValidServerUrl(url: String) {
        _isServerUrlValidateLoading.value = true
        checkValidServerJob?.cancel()
        checkValidServerJob = viewModelScope.launch {
            if (!isValidServerUrl(url)) {
                printlnCK("checkValidServerUrl local validation invalid")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            if (serverRepository.getServerByDomain(url) != null) {
                printlnCK("checkValidServerUrl duplicate server")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            val server = environment.getServer()
            val workspaceInfoResponse = workSpaceRepository.getWorkspaceInfo(server.serverDomain, url)
            _isServerUrlValidateLoading.value = false
            if (workspaceInfoResponse.status == Status.ERROR) {
                printlnCK("checkValidServerUrl invalid server from remote")
                serverUrlValidateResponse.value = ""
                return@launch
            }

            serverUrlValidateResponse.value = url
        }
    }

    fun cancelCheckValidServer() {
        _isServerUrlValidateLoading.value = false
        checkValidServerJob?.cancel()
    }

    fun deleteKey() {
         viewModelScope.launch {
            val index= signalIdentityKeyDAO
                 .deleteSignalKeyByOwnerDomain(
                     clientId = environment.getServer().ownerClientId,
                     environment.getServer().serverDomain
                 )
             printlnCK("delete signalIdentityKeyDAO $index")
            val senderAddress = CKSignalProtocolAddress(
                Owner(
                    currentServer.value!!.serverDomain,
                    currentServer.value!!.ownerClientId
                ), 222
            )

             val test4 = signalPreKeyDAO.deleteSignalSenderKey(
                 currentServer.value!!.serverDomain,
                 currentServer.value!!.ownerClientId
             )

            printlnCK("deleteKey 2 ${currentServer.value!!.serverDomain}   ${test4}")

            chatGroups.value?.forEach { group->
                val groupSender2 = SenderKeyName(group.groupId.toString(), senderAddress)
                senderKeyStore.deleteSenderKey(groupSender2)
                printlnCK("deleteSignalSenderKey2: ${groupSender2.groupId}  ${groupSender2.sender.name}")
                group.clientList.forEach {
                    val senderAddress = CKSignalProtocolAddress(
                        Owner(
                            it.domain,
                            it.userId
                        ), 111
                    )
                    val groupSender = SenderKeyName(group.groupId.toString(), senderAddress)
                    val test2 =
                        signalKeyDAO.deleteSignalSenderKey(groupSender.groupId, groupSender.sender.name)
                    printlnCK("chatGroups signalKey ${groupSender.sender.name} ${test2}")
                }
            }
        }
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()
